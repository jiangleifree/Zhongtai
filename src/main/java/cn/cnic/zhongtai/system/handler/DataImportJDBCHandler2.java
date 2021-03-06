package cn.cnic.zhongtai.system.handler;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.system.config.Launcher;
import cn.cnic.zhongtai.system.dao.HiveDao;
import cn.cnic.zhongtai.system.model.DataImportResult;
import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.model.vo.DataImportJDBCVo;
import cn.cnic.zhongtai.system.model.vo.ImportDataVo;
import cn.cnic.zhongtai.system.model.vo.JDBCConnectionConfig;
import cn.cnic.zhongtai.system.service.DataImportService;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.*;
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
import java.util.*;
import java.util.concurrent.*;

import static cn.cnic.zhongtai.system.Constant.DATAX_JOB_PATH;
import static cn.cnic.zhongtai.system.Constant.HDFS_PATH_PRE;

@Component
@Slf4j
public class DataImportJDBCHandler2 {

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
                String command = "python /data/tobaccoZhongtai/datax-hdfs/datax/bin/datax.py " + jobFilePath;
                //????????????????????????????????????????????????
                String retLog = UnixUtils.run(new String[]{command}, true);
                //TODO ??????????????????????????????????????????
                String[] resultArr = resolveStringLog(retLog);
                if (resultArr[0].equals("-1")){
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

        for (Task task : tasks) {
            fileSystem = hdfsUtil.getFS();
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
            List<DataImportResult> dataImportResults = new ArrayList<>();

            //??????????????????
            for (DataImportJDBCVo.Table temp : tablesArr) {
                ImportDataTask importDataTask = new ImportDataTask(userName, password, jdbcUrl,
                        temp.getTableName(),
                        temp.getImportType(),
                        temp.getHiveTableName());
                Future<DataImportResult> submit = es.submit(importDataTask);
                dataImportFutures.add(submit);
            }

            Set<String> failTableName = new HashSet<>();
            //????????????????????????
            for (Future<DataImportResult> future : dataImportFutures) {
                try {
                    DataImportResult dataImportResult = future.get();
                    //?????????????????????????????????
                    if (dataImportResult.getResult().equals("success")) {
                        dataImportResults.add(dataImportResult);
                    } else { //???????????????????????????????????????????????????
                        failTableName.add(dataImportResult.getTableName());
                        log.error(dataImportResult.getErrMsg());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            es.shutdown();

            if (CollectionUtils.isEmpty(dataImportResults)){
                log.error("????????????????????????:");
                log.error(Strings.join(failTableName, ','));
                //??????????????????
                taskService.changeTaskStatus(task.getTaskId(), "??????jdbc????????????");
                dataImportService.changeDataImportProgress(dataManageId, "??????jdbc????????????");
                return;
            }

            dataImportService.setDataImportResult(dataManageId, StringUtils.join(dataImportResults, ";"));
            //??????????????????
            taskService.changeTaskStatus(task.getTaskId(), "??????jdbc????????????");
            dataImportService.changeDataImportProgress(dataManageId, "??????jdbc????????????");

            for (DataImportResult result : dataImportResults) {
                String type = result.getImportType();
                switch (type) {
                    case "0": //????????????
                        newType(result, task.getTaskId(), dataManageId);
                        break;
                    case "1": //??????
                        appendType(result, task.getTaskId(), dataManageId);
                        break;
                    case "2": //??????
                        coverType(result, task.getTaskId(), dataManageId);
                        break;
                    default: //????????????
                        newType(result, task.getTaskId(), dataManageId);
                }
            }

            try{
                //???????????????????????????, ??????, ????????????
                List<DataImportJDBCVo.Table> tableList = new ArrayList<>();
                for (DataImportResult dataImportResult : dataImportResults) {
                    DataImportJDBCVo.Table table = new DataImportJDBCVo.Table();
                    table.setTableName(dataImportResult.getTableName());
                    table.setHiveTableName(dataImportResult.getHiveTableName());
                    table.setImportType(dataImportResult.getImportType());
                    table.setColumns(dataImportResult.getColumn());
                    tableList.add(table);
                }
                dataImportService.doFinalWork(tableList, pid, jdbcUrl);
            } catch (Exception e) {
                //??????????????????
                taskService.changeTaskStatus(task.getTaskId(), "??????????????????, ??????, ??????????????????");
                dataImportService.changeDataImportProgress(dataManageId, "??????????????????, ??????, ??????????????????");

                //??????hdfs????????????
                hdfsUtil.closeFS(fileSystem);
                return;
            }

            //??????hdfs????????????
            hdfsUtil.closeFS(fileSystem);

            //??????????????????
            taskService.changeTaskStatus(task.getTaskId(), "??????????????????");
            dataImportService.changeDataImportProgress(dataManageId, "??????????????????");

        }

    }

    private void coverType(DataImportResult result, int taskId, Long dataManageId){}

    private void appendType(DataImportResult result, int taskId, Long dataManageId) {

    }

    private void newType(DataImportResult result, int taskId, Long dataManageId){
        //?????????????????????hdfs??????
        List<String> succeedCreatedHdfs = new ArrayList<>();
        try {
            String hql = SqlUtils.generateCreateHiveTableSqlByORC2(result.getHiveTableName(),
                    "",result.getColumn(), result.getHdfsUrl());
            hiveDao.getJdbcTemplate().execute(hql);
            succeedCreatedHdfs.add(result.getHdfsUrl());
        } catch (Exception e) {
            //??????????????????
            taskService.changeTaskStatus(taskId, result.getTableName() + "???????????????");
            dataImportService.changeDataImportProgress(dataManageId, result.getTableName() + "???????????????");
            //?????????????????????hdfs?????????
            succeedCreatedHdfs.stream()
                    .forEach(hdfs -> hdfsUtil.rm(hdfs, true));
            //?????????????????????hdfs??????
            hdfsUtil.rm(result.getHdfsUrl(), true);

            //??????hdfs????????????
            hdfsUtil.closeFS(fileSystem);
            return;
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
        if (flag == -1){ //????????????
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
