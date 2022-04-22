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
            //根据table查询所有的column
            //这里需要jdbc库中的tableName
            List<String> tableColumns = DBUtil.getTableColumns(dataBaseType, jdbcUrl,
                    userName, password, tableName);
            result.setColumn(tableColumns);

            //datax需要的参数
            String[] column = tableColumns.toArray(new String[tableColumns.size()]);
            column = column.clone();

            //定义hdfs路径 /dataFiles/jdbc/xxx  每次操作都新建一个目录
            String hdfsUrl = HDFS_PATH_PRE + "jdbc/" + hiveTableName + Calendar.getInstance().getTimeInMillis();

            //创建hdfs文件夹
            try {
                hdfsUtil.mkdir(fileSystem, hdfsUrl);
                result.setHdfsUrl(hdfsUrl);
            } catch (Exception e) {
                e.printStackTrace();
                result.setResult("failed");
                result.setErrMsg("hdfs创建文件失败");
                return result;
            }

            //拼接datax所需要的配置信息
            //datax使用tableName来获取数据
            ImportDataVo importDataVo = generateImportDataVo(tableName, userName, password, jdbcUrl, column, hdfsUrl);
            JDBCConnectionConfig config = JDBCConnectionConfig.generateConfig(importDataVo);
            String jsonString = JsonUtils.toJsonNoException(config);
            try {
                //创建datax需要的json文件  生成一个不重复复的json文件
                String jobFilePath = DATAX_JOB_PATH + hiveTableName + Calendar.getInstance().getTimeInMillis() + ".json";
                FileUtils.writeJsonToFile(jsonString, jobFilePath);

                //执行datax任务
                String command = "python /data/tobaccoZhongtai/datax-hdfs/datax/bin/datax.py " + jobFilePath;
                //截取一下日志进行判断任务成功失败
                String retLog = UnixUtils.run(new String[]{command}, true);
                //TODO 下一步解析日志来判断任务状态
                String[] resultArr = resolveStringLog(retLog);
                if (resultArr[0].equals("-1")){
                    result.setResult("failed");
                    //result.setErrMsg(retLog);
                } else {
                    result.setResult("success");
                }
                //TODO 如果状态为failed  以下结果是错误的
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

    //实际的任务处理方法
    public void handle() {

        List<Task> tasks = taskService.getTasksByTypeAndStatus(Task.ONLINE_IMPORT_DATA_BY_JDBC, Task.WAITING);

        for (Task task : tasks) {
            fileSystem = hdfsUtil.getFS();
            //获取所需的参数
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

            //修改任务状态为 正在读取jdbc数据
            taskService.changeTaskStatus(task.getTaskId(), "正在读取jdbc数据");
            dataImportService.changeDataImportProgress(dataManageId, "正在读取jdbc数据");

            ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                    .setNameFormat("jdbc-pool-%d").build();
            ExecutorService es = new ThreadPoolExecutor(poolSize, 200,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

            //数据导入结果
            List<Future<DataImportResult>> dataImportFutures = new ArrayList<>();
            List<DataImportResult> dataImportResults = new ArrayList<>();

            //执行导出任务
            for (DataImportJDBCVo.Table temp : tablesArr) {
                ImportDataTask importDataTask = new ImportDataTask(userName, password, jdbcUrl,
                        temp.getTableName(),
                        temp.getImportType(),
                        temp.getHiveTableName());
                Future<DataImportResult> submit = es.submit(importDataTask);
                dataImportFutures.add(submit);
            }

            Set<String> failTableName = new HashSet<>();
            //阻塞获取任务结果
            for (Future<DataImportResult> future : dataImportFutures) {
                try {
                    DataImportResult dataImportResult = future.get();
                    //这里只统计了任务成功的
                    if (dataImportResult.getResult().equals("success")) {
                        dataImportResults.add(dataImportResult);
                    } else { //这里可以根据错误信息进行相应的操作
                        failTableName.add(dataImportResult.getTableName());
                        log.error(dataImportResult.getErrMsg());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            es.shutdown();

            if (CollectionUtils.isEmpty(dataImportResults)){
                log.error("数据导出失败的表:");
                log.error(Strings.join(failTableName, ','));
                //任务状态修改
                taskService.changeTaskStatus(task.getTaskId(), "读取jdbc数据失败");
                dataImportService.changeDataImportProgress(dataManageId, "读取jdbc数据失败");
                return;
            }

            dataImportService.setDataImportResult(dataManageId, StringUtils.join(dataImportResults, ";"));
            //任务状态修改
            taskService.changeTaskStatus(task.getTaskId(), "读取jdbc数据完成");
            dataImportService.changeDataImportProgress(dataManageId, "读取jdbc数据完成");

            for (DataImportResult result : dataImportResults) {
                String type = result.getImportType();
                switch (type) {
                    case "0": //新建方式
                        newType(result, task.getTaskId(), dataManageId);
                        break;
                    case "1": //追加
                        appendType(result, task.getTaskId(), dataManageId);
                        break;
                    case "2": //覆盖
                        coverType(result, task.getTaskId(), dataManageId);
                        break;
                    default: //默认新建
                        newType(result, task.getTaskId(), dataManageId);
                }
            }

            try{
                //创建需要的虚拟目录, 模型, 血缘关系
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
                //任务状态修改
                taskService.changeTaskStatus(task.getTaskId(), "创建虚拟目录, 模型, 血缘关系失败");
                dataImportService.changeDataImportProgress(dataManageId, "创建虚拟目录, 模型, 血缘关系失败");

                //关闭hdfs操作对象
                hdfsUtil.closeFS(fileSystem);
                return;
            }

            //关闭hdfs操作对象
            hdfsUtil.closeFS(fileSystem);

            //任务状态修改
            taskService.changeTaskStatus(task.getTaskId(), "数据导入成功");
            dataImportService.changeDataImportProgress(dataManageId, "数据导入成功");

        }

    }

    private void coverType(DataImportResult result, int taskId, Long dataManageId){}

    private void appendType(DataImportResult result, int taskId, Long dataManageId) {

    }

    private void newType(DataImportResult result, int taskId, Long dataManageId){
        //已经成功上传的hdfs文件
        List<String> succeedCreatedHdfs = new ArrayList<>();
        try {
            String hql = SqlUtils.generateCreateHiveTableSqlByORC2(result.getHiveTableName(),
                    "",result.getColumn(), result.getHdfsUrl());
            hiveDao.getJdbcTemplate().execute(hql);
            succeedCreatedHdfs.add(result.getHdfsUrl());
        } catch (Exception e) {
            //任务状态修改
            taskService.changeTaskStatus(taskId, result.getTableName() + "表创建失败");
            dataImportService.changeDataImportProgress(dataManageId, result.getTableName() + "表创建失败");
            //删除已经上传到hdfs的文件
            succeedCreatedHdfs.stream()
                    .forEach(hdfs -> hdfsUtil.rm(hdfs, true));
            //删除本次失败的hdfs文件
            hdfsUtil.rm(result.getHdfsUrl(), true);

            //关闭hdfs操作对象
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
        //创建importDataVo对象 为生成json文件

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

    //解析日志
    private String[] resolveStringLog(String log) {

        String readTotal = "-1";
        int flag = log.indexOf("读出记录总数");
        if (flag == -1){ //任务失败
            return new String[]{"-1", "-1"};
        }
        String[] line = log.split("\n");
        String readTotalLine = line[line.length - 2];
        String[] split = readTotalLine.split(":");
        if (split[0].contains("读出记录总数")) {
            readTotal = split[1].trim();
        }

        String faliTotalLine = line[line.length - 1];
        String failTotal = faliTotalLine.split(":")[1].trim();
        return new String[]{readTotal, failTotal};

    }

}
