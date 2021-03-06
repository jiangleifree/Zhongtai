package cn.cnic.zhongtai.system.handler;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.system.config.Launcher;
import cn.cnic.zhongtai.system.config.jdbcConfig.HiveJdbcBaseDaoImpl;
import cn.cnic.zhongtai.system.listener.InputHiveTaskListener;
import cn.cnic.zhongtai.system.model.*;
import cn.cnic.zhongtai.system.model.vo.DataImportJDBCVo;
import cn.cnic.zhongtai.system.neo4j.Neo4jFactory;
import cn.cnic.zhongtai.system.service.DataImportService;
import cn.cnic.zhongtai.system.service.DataMappingService;
import cn.cnic.zhongtai.system.service.GenTableService;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.IOUtils;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cn.cnic.zhongtai.system.Constant.SPARK_JAR_INPUT_DATA;

@Component
@Slf4j
public class Neo4jToHiveHandler {

    @Resource
    private TaskService taskService;
    @Resource
    private HdfsUtil hdfsUtil;
    @Resource
    private SqlUtils sqlUtils;
    @Resource
    private HiveJdbcBaseDaoImpl hiveJdbcBaseDaoImpl;
    @Resource
    private Launcher launcher;
    @Resource
    private DataImportService dataImportService;
    @Resource
    private GenTableService genTableService;
    @Resource
    private DataMappingService dataMappingService;
    private int poolSize = Runtime.getRuntime().availableProcessors() * 2;
    private FileSystem fs;
    private static final int LIMIT = 100000;

    public void handler() {
        List<Task> tasks = taskService.getTasksByTypeAndStatus(Task.NEO4J_TO_HIVE_TASK, Task.WAITING);
        for (Task task : tasks) {

            String jsonData = task.getJsonData();
            JSONObject param = JSONObject.parseObject(jsonData);
            String boltUrl = param.getString("boltUrl");
            String userName = param.getString("userName");
            String password = param.getString("password");
            Long dataManageId = param.getLong("dataManageId");
            JSONArray labels = param.getJSONArray("labels");
            JSONArray relations = param.getJSONArray("relations");
            String pid = param.getString("pid");

            //????????????????????? ????????????neo4j??????
            taskService.changeTaskStatus(task.getTaskId(), "????????????neo4j??????");
            dataImportService.changeDataImportProgress(dataManageId, "????????????neo4j??????");

            //Session session = Neo4JUtil.getSession(boltUrl, userName, password);
            Driver driver = Neo4jFactory.getDriverFromPool(boltUrl).orElseGet(new Supplier<Driver>() {
                @Override
                public Driver get() {
                    Driver ret = Neo4jFactory.newDriver(boltUrl, userName, password).get();
                    Neo4jFactory.addDriverToPool(boltUrl, ret);
                    return ret;
                }
            });
            Session session = driver.session();

            ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                    .setNameFormat("neo4j-pool-%d").build();
            ExecutorService es = new ThreadPoolExecutor(poolSize, 200,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

            fs = hdfsUtil.getFS();
            //???????????????null
            boolean labelsIsEmpty = CollectionUtils.isEmpty(labels);
            boolean relationsIsEmpty = CollectionUtils.isEmpty(relations);

            Map<String, Set<Neo4jTypeProperty>> nodeTypeProperties = Neo4JUtil.getNodeTypeProperties(session);

            List<Future<Neo4jDataImportResult>> futures = new ArrayList<>();
            List<Neo4jDataImportResult> results = new ArrayList<>();
            //??????NodeTask
            for (int i = 0; !labelsIsEmpty && i < labels.size(); i++) {
                JSONObject label = labels.getJSONObject(i);

                Neo4jTaskResult neo4jTaskResult = new Neo4jTaskResult();
                String hiveTableName = label.getString("hiveTableName");
                //??????hdfs??????
                String hdfsUrl = "";
                try {
                    hdfsUrl = createHdfsDir(fs, hiveTableName);
                    neo4jTaskResult.setCreateHdfsDir(true);
                } catch (Exception e) {
                    neo4jTaskResult.setCreateHdfsDir(false);
                }
                String labelName = label.getString("tableName");
                List<String> nodeFields = nodeTypeProperties.getOrDefault(labelName, new HashSet<>())
                        .stream()
                        .map(nodeTypeProperty -> nodeTypeProperty.getPropertyName())
                        .collect(Collectors.toList());
                nodeFields.add("neo4j_id");

                if (neo4jTaskResult.isCreateHdfsDir()) { //hive?????????????????????hive???????????????
                    //?????????????????????  ?????????????????????
                    int count = Neo4JUtil.getCountByLabel(session, label.getString("tableName"));
                    List<CqlQueryBody> queryBodies = CqlQueryBody.generate(count, LIMIT, label.getString("tableName"));
                    for (CqlQueryBody queryBody : queryBodies) {
                        futures.add(es.submit(new NodeTask(
                                session, queryBody, fs, hdfsUrl, nodeFields,
                                hiveTableName, label.getString("importType"))));
                    }
                }
            }
            //??????RelationTask
            for (int i = 0; !relationsIsEmpty && i < relations.size(); i++) {
                JSONObject relation = relations.getJSONObject(i);
                Neo4jTaskResult neo4jTaskResult = new Neo4jTaskResult();
                String hiveTableName = relation.getString("hiveTableName");
                //??????hdfs??????
                String hdfsUrl = "";
                try {
                    hdfsUrl = createHdfsDir(fs, hiveTableName);
                    neo4jTaskResult.setCreateHdfsDir(true);
                } catch (Exception e) {
                    neo4jTaskResult.setCreateHdfsDir(false);
                }
                //??????{relationshipType}?????????????????? (start)-[relationshipType]->(end)
                List<RelationShip> relationShips = Neo4JUtil.getRelationShips(session, relation.getString("tableName"));
                //???????????????????????????  ???????????????????????????
                RelationShip relationShip = relationShips.get(0);

                //????????????
                List<String> relationFields = Neo4JUtil.getRelationFields(session, relationShip);

                if (neo4jTaskResult.isCreateHdfsDir()) { //hive?????????????????????hive???????????????
                    //?????????????????????  ?????????????????????
                    int countByRelation = Neo4JUtil.getCountByRelation(session, relationShip);
                    List<CqlQueryBody> queryBodies = CqlQueryBody.generate(countByRelation, LIMIT, relationShip);
                    for (CqlQueryBody queryBody : queryBodies) {
                        futures.add(es.submit(new RelationTask(session,
                                queryBody, fs, hdfsUrl, relationFields,
                                hiveTableName, relation.getString("importType"))));
                    }
                }
            }

            // ????????????????????????10000?????????????????????????????????
            // ???????????????map?????????hiveTableName??????
            Map<String, Neo4jDataImportResult> resultMap = new HashMap<>();
            for (Future<Neo4jDataImportResult> future : futures) {
                try {
                    Neo4jDataImportResult result = future.get();
                    //?????????????????????????????????, ?????????????????????
                    if (result.getResult().equals("success")) {
                        //results.add(result);
                        resultMap.put(result.getHiveTableName(), result);
                    } else {
                        log.error(result.getErrMsg());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            results = new ArrayList<>(resultMap.values());

            List<SparkParam> sparkParams = new ArrayList<>();
            List<DataImportJDBCVo.Table> tableListListener = new ArrayList<>(); //??????listener

            //????????????????????????
            for (Neo4jDataImportResult result : results) {
                SparkParam sparkParam = new SparkParam();
                sparkParam.setHiveTableName(result.getHiveTableName());
                sparkParam.setHdfs(result.getHdfsUrl());
                sparkParam.setColumn(result.getColumns());
                sparkParam.setSuffix("json");
                sparkParam.setSign(Constant.HDFS_DELIMITER);
                sparkParam.setType(result.getImportType());
                sparkParams.add(sparkParam);

                DataImportJDBCVo.Table table = new DataImportJDBCVo.Table();
                table.setTableName(result.getTableName());
                table.setHiveTableName(result.getHiveTableName());
                table.setImportType(result.getImportType());
                table.setColumns(result.getColumns());
                tableListListener.add(table);
            }
            es.shutdown();

            //??????results??????, ????????????????????????
            if (CollectionUtils.isEmpty(results)) {
                //??????????????????
                taskService.changeTaskStatus(task.getTaskId(), "??????neo4j????????????");
                dataImportService.changeDataImportProgress(dataManageId, "??????neo4j????????????");

                //????????????
                Neo4JUtil.closeSession(session);
                IOUtils.closeStream(fs);
                hdfsUtil.closeFS(fs);
                return;
            }
            //??????spark??????
            String stdputLogPath = "/data/tobaccoZhongtai/spark/log/zhongtai" + CommonUtils.getUUID32() + ".log";
            InputHiveTaskListener inputHiveTaskListener = new InputHiveTaskListener(String.valueOf(task.getTaskId()),
                    stdputLogPath, "", dataManageId, tableListListener, boltUrl, pid);
            Map<String, Object> sparkParam = new HashMap<>();
            sparkParam.put("tables", sparkParams);
            String args = JSON.toJSONString(sparkParam);
            //???????????????
            File logFile = new File(stdputLogPath);
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //??????spark??????
            try {
                launcher.CreateSparkLauncher(SPARK_JAR_INPUT_DATA, args, inputHiveTaskListener, logFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //??????????????????
            taskService.changeTaskStatus(task.getTaskId(), "??????neo4j????????????");
            dataImportService.changeDataImportProgress(dataManageId, "??????neo4j????????????");

            //????????????
            Neo4JUtil.closeSession(session);
            IOUtils.closeStream(fs);
            hdfsUtil.closeFS(fs);
        }
    }

    private String createHdfsDir(FileSystem fs, String hiveTableName) {

        String hdfsUrl = "/dataFiles/neo4j/" + hiveTableName + Calendar.getInstance().getTimeInMillis();
        try {
            hdfsUtil.mkdir(fs, hdfsUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hdfsUrl;
    }

    private class RelationTask implements Callable<Neo4jDataImportResult> {
        private CqlQueryBody cqlQueryBody;
        private Session session;
        private FileSystem fs;
        private String hdfsUrl;
        private List<String> relationFields;
        private String hiveTableName;
        private String importType;

        public RelationTask() {
        }

        public RelationTask(Session session, CqlQueryBody cqlQueryBody,
                            FileSystem fs, String hdfsUrl, List<String> relationFields,
                            String hiveTableName, String importType) {
            this.session = session;
            this.cqlQueryBody = cqlQueryBody;
            this.fs = fs;
            this.hdfsUrl = hdfsUrl;
            this.relationFields = relationFields;
            this.hiveTableName = hiveTableName;
            this.importType = importType;
        }

        @Override
        public Neo4jDataImportResult call() throws Exception {
            Neo4jDataImportResult result = new Neo4jDataImportResult();
            List<Map<String, Object>> data = null;
            try {
                data = Neo4JUtil.getRelationData(session, cqlQueryBody);
            } catch (Exception e) {
                result.setResult("failed");
                result.setErrMsg("cypher????????????");
                return result;
            }
            File jsonFile = null;
            try {

                jsonFile = exportJsonFile(data, "/data/temp/json/", cqlQueryBody.getRelationShip().getRelationShip() + CommonUtils.getUUID32());
            } catch (Exception e) {
                result.setResult("failed");
                result.setErrMsg("???????????????json????????????");
                return result;
            }
            try {
                //hdfs??????csv
                hdfsUtil.put(fs, jsonFile.getPath(), hdfsUrl + "/" + jsonFile.getName());
            } catch (Exception e) {
                result.setResult("failed");
                result.setErrMsg("??????hdfs??????");
                return result;
            }

            result.setResult("success");
            result.setColumns(relationFields);
            result.setHdfsUrl(hdfsUrl);
            result.setHiveTableName(hiveTableName);
            result.setTableName(cqlQueryBody.getRelationShip().getRelationShip());
            result.setImportType(importType);
            result.setType("relation");
            return result;
        }
    }

    private class NodeTask implements Callable<Neo4jDataImportResult> {
        private CqlQueryBody cqlQueryBody;
        private Session session;
        private FileSystem fs;
        private String hdfsUrl;
        private List<String> nodeFields;
        private String hiveTableName;
        private String importType;

        public NodeTask() {
        }

        public NodeTask(Session session, CqlQueryBody cqlQueryBody,
                        FileSystem fs, String hdfsUrl,
                        List<String> nodeFields, String hiveTableName,
                        String importType) {
            this.session = session;
            this.cqlQueryBody = cqlQueryBody;
            this.fs = fs;
            this.hdfsUrl = hdfsUrl;
            this.nodeFields = nodeFields;
            this.hiveTableName = hiveTableName;
            this.importType = importType;
        }

        @Override
        public Neo4jDataImportResult call() throws Exception {
            Neo4jDataImportResult result = new Neo4jDataImportResult();
            List<Map<String, Object>> data = null;
            try {
                data = Neo4JUtil.getNodeData(session, cqlQueryBody);
            } catch (Exception e) {
                result.setResult("failed");
                result.setErrMsg("cypher????????????");
                return result;
            }
            File jsonFile = null;
            try {
                jsonFile = exportJsonFile(data, "/data/temp/json/", cqlQueryBody.getLabel() + CommonUtils.getUUID32());
            } catch (Exception e) {
                result.setResult("failed");
                result.setErrMsg("???????????????json????????????");
                return result;
            }
            try {
                hdfsUtil.put(fs, jsonFile.getPath(), hdfsUrl + "/" + jsonFile.getName());
            } catch (Exception e) {
                result.setResult("failed");
                result.setErrMsg("??????hdfs????????????");
                return result;
            }
            result.setResult("success");
            result.setColumns(nodeFields);
            result.setHdfsUrl(hdfsUrl);
            result.setHiveTableName(hiveTableName);
            result.setTableName(cqlQueryBody.getLabel());
            result.setImportType(importType);
            result.setType("label");

            return result;
        }
    }

    private static File exportJsonFile(List<Map<String, Object>> dataList, String outPutPath, String filename) {
        File jsonFile = null;
        try {
            //???????????????
            jsonFile = new File(outPutPath + File.separator + filename + ".json");
            JSONArray jsonArray = new JSONArray();
            jsonArray.addAll(dataList);
            //???json??????
            FileUtils.writeJsonToFile(jsonArray.toJSONString(), jsonFile.getPath());
        } catch (Exception e) {

        }
        return jsonFile;
    }
}
