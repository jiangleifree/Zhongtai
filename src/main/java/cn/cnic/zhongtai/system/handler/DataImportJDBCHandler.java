package cn.cnic.zhongtai.system.handler;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.system.config.Launcher;
import cn.cnic.zhongtai.system.dao.HiveDao;
import cn.cnic.zhongtai.system.listener.InputHiveTaskListener;
import cn.cnic.zhongtai.system.model.DataImportResult;
import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.model.vo.DataImportJDBCVo;
import cn.cnic.zhongtai.system.model.vo.ImportDataVo;
import cn.cnic.zhongtai.system.model.vo.JDBCConnectionConfig;
import cn.cnic.zhongtai.system.service.DataImportService;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static cn.cnic.zhongtai.system.Constant.*;

@Component
@Slf4j
public class DataImportJDBCHandler {

    @Resource
    private TaskService taskService;
    @Resource
    private HdfsUtil hdfsUtil;
    @Resource
    private Launcher launcher;
    @Resource
    private DataImportService dataImportService;
    @Resource
    private HiveDao hiveDao;
    private FileSystem fileSystem;
    private int poolSize = Runtime.getRuntime().availableProcessors() * 2;

    private class ImportDataTask implements Callable<DataImportResult> {
        private String userName;
        private String password;
        private String jdbcUrl;
        private String tableName;
        private String importType;
        private String hiveTableName;

        public ImportDataTask() {
        }

        public ImportDataTask(String userName, String password, String jdbcUrl,
                              String tableName, String importType, String hiveTableName) {
            this.userName = userName;
            this.password = password;
            this.jdbcUrl = jdbcUrl;
            this.tableName = tableName;
            this.importType = importType;
            this.hiveTableName = hiveTableName;
        }

        @Override
        public DataImportResult call() {
            DataImportResult result = new DataImportResult();

            result.setHiveTableName(hiveTableName);
            result.setTableName(tableName);
            result.setImportType(importType);
            DataBaseType dataBaseType = DataBaseType.retDataBaseType(jdbcUrl);
            //??????table???????????????column
            //????????????jdbc?????????tableName
            List<String> tableColumns = DBUtil.getTableColumns(dataBaseType, jdbcUrl,
                    userName, password, tableName);
            result.setColumn(tableColumns);

            //datax???????????????
            String[] column = tableColumns.toArray(new String[tableColumns.size()]);
            column = column.clone();

            //??????hdfs?????? /dataFiles/jdbc/xxx  ?????????????????????????????????
            String hdfsUrl = HDFS_PATH_PRE + "jdbc/" + hiveTableName + Calendar.getInstance().getTimeInMillis();

            //??????hdfs?????????
            try {
                hdfsUtil.mkdir(fileSystem, hdfsUrl);
                result.setHdfsUrl(hdfsUrl);
            } catch (Exception e) {
                e.printStackTrace();
                result.setResult("failed");
                result.setErrMsg("hdfs??????????????????");
                return result;
            }

            //??????datax????????????????????????
            //datax??????tableName???????????????
            ImportDataVo importDataVo = generateImportDataVo(tableName, userName, password, jdbcUrl, column, hdfsUrl);
            JDBCConnectionConfig config = JDBCConnectionConfig.generateConfig(importDataVo);
            String jsonString = JsonUtils.toJsonNoException(config);
            try {
                //??????datax?????????json??????  ???????????????????????????json??????
                String jobFilePath = DATAX_JOB_PATH + hiveTableName + Calendar.getInstance().getTimeInMillis() + ".json";
                FileUtils.writeJsonToFile(jsonString, jobFilePath);

                //??????datax??????
                String command = "python /data/tobaccoZhongtai/datax/datax/bin/datax.py " + jobFilePath;
                //String command = "python /data/tobaccoZhongtai/datax-hdfs/datax/bin/datax.py " + jobFilePath;
                //????????????????????????????????????????????????
                String retLog = UnixUtils.run(new String[]{command}, true);
                //TODO ??????????????????????????????????????????
                String[] resultArr = resolveStringLog(retLog);
                if (resultArr[0].equals("-1")) {
                    result.setResult("failed");
                    //result.setErrMsg(retLog);
                } else {
                    result.setResult("success");
                }
                //TODO ???????????????failed  ????????????????????????
                result.setGetTotal(resultArr[0]);
                result.setFailTotal(resultArr[1]);
                return result;

            } catch (Exception e) {
                e.printStackTrace();
                result.setResult("failed");
                result.setErrMsg(e.getMessage());
                return result;
            }
        }
    }

    //???????????????????????????
    public void handle() {

        List<Task> tasks = taskService.getTasksByTypeAndStatus(Task.ONLINE_IMPORT_DATA_BY_JDBC, Task.WAITING);
        //List<Task> tasks = taskService.getTasksByTypeAndStatus(Task.ONLINE_IMPORT_DATA_BY_JDBC, "test");
        if (tasks.size() > 0) {
            //??????hdfs????????????
            fileSystem = hdfsUtil.getFS();
        }
        for (Task task : tasks) {
            //?????????????????????
            JSONObject param = JSONObject.parseObject(task.getJsonData());
            String jdbcUrl = param.getString("jdbcUrl");
            String userName = param.getString("userName");
            String password = param.getString("password");
            Long dataManageId = param.getLong("dataManageId");
            JSONArray tables = param.getJSONArray("tables");
            DataImportJDBCVo.Table[] tablesArr = new DataImportJDBCVo.Table[tables.size()];
            for (int i = 0; i < tables.size(); i++) {
                tablesArr[i] =
                        tables.getJSONObject(i).toJavaObject(DataImportJDBCVo.Table.class);
            }
            String pid = param.getString("pid");

            //????????????????????? ????????????jdbc??????
            taskService.changeTaskStatus(task.getTaskId(), "????????????jdbc??????");
            dataImportService.changeDataImportProgress(dataManageId, "????????????jdbc??????");

            ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                    .setNameFormat("jdbc-pool-%d").build();
            ExecutorService es = new ThreadPoolExecutor(poolSize, 200,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

            //??????????????????
            List<Future<DataImportResult>> dataImportFutures = new ArrayList<>();
            List<DataImportResult> successDataImportResults = new ArrayList<>();
            Set<String> failTableName = new HashSet<>();

            //??????????????????
            for (DataImportJDBCVo.Table temp : tablesArr) {
                ImportDataTask importDataTask = new ImportDataTask(userName, password, jdbcUrl,
                        temp.getTableName(),
                        temp.getImportType(),
                        temp.getHiveTableName());
                Future<DataImportResult> submit = es.submit(importDataTask);
                dataImportFutures.add(submit);
            }
            //????????????????????????
            for (Future<DataImportResult> future : dataImportFutures) {
                try {
                    DataImportResult dataImportResult = future.get();
                    //?????????????????????????????????
                    if (dataImportResult.getResult().equals("success")) {
                        successDataImportResults.add(dataImportResult);
                    } else { //???????????????????????????????????????????????????
                        failTableName.add(dataImportResult.getTableName());
                        log.error(dataImportResult.getErrMsg());
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            es.shutdown();

            if (!CollectionUtils.isEmpty(failTableName)) {
                log.error("????????????????????????:");
                log.error(Strings.join(failTableName, ','));
            }

            //jdbc??????????????????
            //TODO ????????????????????????, ??????????????????????????????????????????
            if (CollectionUtils.isEmpty(successDataImportResults)) {
                //??????????????????
                taskService.changeTaskStatus(task.getTaskId(), "??????jdbc????????????");
                dataImportService.changeDataImportProgress(dataManageId, "??????jdbc????????????");
                return;
            }

            //List<SparkParam> sparkParams = new ArrayList<>();
            List<Map<String, Object>> sparkParams = new ArrayList<>();
            List<DataImportJDBCVo.Table> tableListListener = new ArrayList<>(); //?????????listener?????????
            for (int i = 0; i < successDataImportResults.size(); i++) {

                DataImportResult dataImportResult = successDataImportResults.get(i);
                if ("0".equals(dataImportResult.getGetTotal())) { //???????????????, ????????????spark, ????????????
                    emptyTableOperation(dataImportResult, pid, jdbcUrl);
                    continue;
                }

                Map<String, Object> sparkParam = new HashMap<>();
                sparkParam.put("path", dataImportResult.getHdfsUrl());
                sparkParam.put("type", dataImportResult.getImportType());
                sparkParam.put("hiveTableName", dataImportResult.getHiveTableName());
                sparkParams.add(sparkParam);

                DataImportJDBCVo.Table table = new DataImportJDBCVo.Table();
                table.setTableName(dataImportResult.getTableName());
                table.setHiveTableName(dataImportResult.getHiveTableName());
                table.setImportType(dataImportResult.getImportType());
                table.setColumns(dataImportResult.getColumn());
                tableListListener.add(table);
            }

            //??????????????? spark?????????????????????
            String stdoutLogPath = SPARK_IMPORT_DATA_TASK_LOG_PATH + CommonUtils.getUUID32() + ".log";
            File stdoutLogFile = new File(stdoutLogPath);
            try {
                stdoutLogFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (CollectionUtils.isEmpty(sparkParams) || CollectionUtils.isEmpty(tableListListener)) {
                //?????????????????????empty, ????????????
            } else {
                InputHiveTaskListener inputHiveTaskListener = new InputHiveTaskListener(String.valueOf(task.getTaskId()),
                        stdoutLogPath, "", dataManageId, tableListListener, jdbcUrl, pid);
                //spark jar?????????
                Map<String, Object> sparkParam = new HashMap<>();
                sparkParam.put("tables", sparkParams);
                String args = JSON.toJSONString(sparkParam);
                //??????spark??????
                try {
                    launcher.CreateSparkLauncher(SPARK_JAR_ORC_TO_HIVE, args, inputHiveTaskListener, stdoutLogFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            dataImportService.setDataImportResult(dataManageId, StringUtils.join(successDataImportResults, ";"));
            //??????????????????
            taskService.changeTaskStatus(task.getTaskId(), "??????jdbc????????????");
            dataImportService.changeDataImportProgress(dataManageId, "??????jdbc????????????");

            //??????hdfs????????????
            hdfsUtil.closeFS(fileSystem);
        }

    }

    /**
     * ??????????????????
     * @param result
     */
    private void emptyTableOperation(DataImportResult result, String pid, String jdbcUrl){

        try {
            String importType = result.getImportType();
            //??????????????????, importType="2"????????????????????????, ??????,???????????????????????? "0","1"/??????,???????????????
            if ("2".equals(importType)) {
                String hql = SqlUtils.generateCreateHiveTableSqlByORC2(result.getHiveTableName(),
                        "",result.getColumn(), result.getHdfsUrl());
                hiveDao.getJdbcTemplate().execute(hql);

                //???????????????????????????, ??????, ????????????
                List<DataImportJDBCVo.Table> tableList = new ArrayList<>();
                DataImportJDBCVo.Table table = new DataImportJDBCVo.Table();
                table.setTableName(result.getTableName());
                table.setHiveTableName(result.getHiveTableName());
                table.setImportType(result.getImportType());
                table.setColumns(result.getColumn());
                tableList.add(table);

                dataImportService.doFinalWork(tableList, pid, jdbcUrl);
            }
        } catch (Exception e) {
            log.error(String.format("table: {} create failed"), result.getHiveTableName());
        }
    }

    private ImportDataVo generateImportDataVo(String tableName, String userName, String password, String jdbcUrl, String[] column, String hdfsPath) {
        JSONArray hdfsColumns = new JSONArray();
        ImportDataVo importDataVo = new ImportDataVo();
        for (int i = 0; i < column.length; i++) {

            Map<String, String> hdfsColumn = new HashMap<>();
            hdfsColumn.put("name", column[i]);
            hdfsColumn.put("type", "string");
            hdfsColumns.set(i, hdfsColumn);
        }
        String dataBaseTypeStr = jdbcUrl.split(":")[1];
        switch (dataBaseTypeStr) {
            case "mysql":
                importDataVo.setEngine("mysqlreader");
                break;
            case "postgresql":
                importDataVo.setEngine("postgresqlreader");
                break;
            case "oracle":
                importDataVo.setEngine("oraclereader");
                break;
            case "sqlserver":
                importDataVo.setEngine("sqlserverreader");
                break;
        }
        //??????importDataVo?????? ?????????json??????

        importDataVo.setTable(tableName);
        importDataVo.setJdbcUrl(jdbcUrl);
        importDataVo.setUserName(userName);
        importDataVo.setPassword(password);
        importDataVo.setHdfsColumns(hdfsColumns);
        importDataVo.setFileName(tableName);
        importDataVo.setColumn(new String[]{"*"});
        importDataVo.setDefaultFS(Constant.DEFAULT_HDFS);
        importDataVo.setHdfsPath(hdfsPath);
        return importDataVo;
    }

    //????????????
    private String[] resolveStringLog(String log) {

        String readTotal = "-1";
        int flag = log.indexOf("??????????????????");
        if (flag == -1) { //????????????
            return new String[]{"-1", "-1"};
        }
        String[] line = log.split("\n");
        String readTotalLine = line[line.length - 2];
        String[] split = readTotalLine.split(":");
        if (split[0].contains("??????????????????")) {
            readTotal = split[1].trim();
        }

        String faliTotalLine = line[line.length - 1];
        String failTotal = faliTotalLine.split(":")[1].trim();
        return new String[]{readTotal, failTotal};

    }

}
