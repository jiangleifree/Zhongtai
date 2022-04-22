package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.system.config.Launcher;
import cn.cnic.zhongtai.system.config.jdbcConfig.HiveJdbcBaseDaoImpl;
import cn.cnic.zhongtai.system.entity.node.NodeGraph;
import cn.cnic.zhongtai.system.listener.TaskListener;
import cn.cnic.zhongtai.system.mapper.DataManageMapper;
import cn.cnic.zhongtai.system.mapper.GenTableColumnMapper;
import cn.cnic.zhongtai.system.mapper.GenTableMapper;
import cn.cnic.zhongtai.system.mapper.TaskMapper;
import cn.cnic.zhongtai.system.model.*;
import cn.cnic.zhongtai.system.model.vo.DataAddToFtpVo;
import cn.cnic.zhongtai.system.model.vo.DataImportJDBCVo;
import cn.cnic.zhongtai.system.model.vo.DataManualFileImportVo;
import cn.cnic.zhongtai.system.model.vo.HttptoDataVo;
import cn.cnic.zhongtai.system.service.*;
import cn.cnic.zhongtai.utils.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.driver.v1.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

import static cn.cnic.zhongtai.system.Constant.SPARK_JAR_INPUT_DATA;

@Transactional
@Service
@Slf4j
public class DataImportServiceImpl implements DataImportService {


    @Value("${syspara.csvPath}")
    private String csvPath;

    @Value("${syspara.jsonPath}")
    private String jsonPath;

    @Value("${syspara.DbName}")
    private String DbName;

    @Value("${syspara.HiveDbName}")
    private String HiveDbName;

    @Value("${syspara.ftpCsvPath}")
    private String ftpCsvPath;

    @Value("${syspara.ftpJsonPath}")
    private String ftpJsonPath;

    @Value("${syspara.TransferStation}")
    private String TransferStation;

    //hdfs文件目录
    @Value("${syspara.hdfsOnlineFile}")
    private String hdfsOnlineFile;
    @Value("${syspara.hdfsOfflineFile}")
    private String hdfsOfflineFile;


    /*服务器地址*/
    @Value("${hiveServer.ip}")
    private String ip;
    @Value("${hiveServer.port}")
    private int port;
    @Value("${hiveServer.username}")
    private String username;
    @Value("${hiveServer.password}")
    private String password;

    @Resource
    private HdfsUtil hdfsUtil;

    @Resource
    private GenTableMapper genTableMapper;
    @Resource
    private GenTableColumnMapper genTableColumnMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Resource
    private GenTableService genTableService;

    @Resource
    private DataManageMapper dataManageMapper;

    @Autowired
    private HiveJdbcBaseDaoImpl hiveJdbcBaseDaoImpl;

    @Resource
    private DataMappingService dataMappingService;

    @Resource
    private DataChangeLogService dataChangeLogService;

    @Resource
    private SqlUtils sqlUtils;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private HiveService hiveService;

    @Resource
    private GraphService graphService;

    @Resource
    private Launcher launcher;


    /**
     * 手工导入
     *
     * @param dataManualFile
     * @return
     */
    public Map<String, Object> setDataFile(DataManualFileImportVo dataManualFile) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 500);
        String fileName = dataManualFile.getFile().getOriginalFilename();
        int one = fileName.lastIndexOf(".");
        //文件的后缀格式类型
        String Suffix = fileName.substring((one + 1), fileName.length());
        if (!Suffix.equals(dataManualFile.getFileType())) {
            throw new RuntimeException("您选中的数据类型与指定文件格式不同");
        }
        map = fileProcessing(Constant.OFFLINE, dataManualFile);
        return map;
    }


    /**
     * 调用spark前需要准备的参数：用来导入数据
     *
     * @param table      表名
     * @param filePath   HDFS路径
     * @param columnlist 表字段
     * @param suffix     文件格式
     * @return
     */
    private JSONObject createJSONObject(String table, String filePath, List<GenTableColumn> columnlist, String type, String suffix) {

        JSONObject result = new JSONObject();
        JSONObject obj = new JSONObject();
        JSONArray json = new JSONArray();

        JSONArray column = new JSONArray();
        for (int i = 0; i < columnlist.size(); i++) {
            column.add(i, columnlist.get(i).getColumnName());
        }

        obj.put("hiveTableName", table);//hive 表名
        //obj.put("hvieTableName", table+"_"+CommonUtils.getCode(4));//hive 表名
        obj.put("hdfs", filePath);//hdfs路径
        //追加/覆盖/新建 ：0/1/2
//        if(isNo){
//            if("append".equals(processingMode)){ //增量
//                obj.put("type", "0");
//            }else  if("cover".equals(processingMode)){ //覆盖
//                obj.put("type", "1");
//            }
//        }else{
//            obj.put("type", "2");
//        }
        obj.put("type", type); //追加/覆盖/新建 ：0/1/2
        obj.put("suffix", suffix);//文件格式
        obj.put("column", column);//字段
        obj.put("sign", ",");//隔离符
        obj.put("source", "RECORD");//xml的标签名,目前其他格式还没用到
        json.add(obj);
        result.put("tables", json);//库.表名
        log.info("脚本：" + result.toString());
        return result;
    }


    /**
     * 创建任务记录
     * @param tableName
     * @param jarPath
     * @param param
     * @param id
     * @param type 什么方式的接入
     * @return
     */
    public int createTask(String tableName, String jarPath, String param, Long id,String type) {
        // createTask(genTable.getTableName(),jars, obj.toString(),dataManage.getId());
        Map<String, String> jsonParam = new HashMap<>();
        jsonParam.put("jarPath", jarPath);
        jsonParam.put("param", param);
        jsonParam.put("type", "dataToHiveByHdfsTask");
        jsonParam.put("id", String.valueOf(id));

        Task task = new Task();
        task.setCreateTime(new Date());
        if("http".equals(type)){
            task.setTaskType("dataToHiveByHttpTask");
        }else {
            task.setTaskType("dataToHiveByHdfsTask");
        }
        task.setTaskName(tableName + "-dataImport");
        task.setStatus(Task.WAITING);
        task.setJsonData(JSON.toJSONString(jsonParam));
        taskMapper.createTask(task);
        return task.getTaskId();
    }

    /**
     * 先判断hive库中表是否存在,然后在生成表的sql
     *
     * @param tableId
     * @return
     */
    public boolean createTableByHiveDb(String tableId) {
        GenTable genTable = genTableMapper.selectByPrimaryKey(Long.valueOf(tableId));
        //1.首先判断这个表是否存在
        //boolean rows = validateTableNameExist(genTable.getTableName());
        boolean rows = hiveService.validateTableNameExistByHive(genTable.getTableName());
        if (!rows) {
            //1先根据table的id去查对应的模型字段表,查到所有的字段信息
            List<GenTableColumn> GenTableColumnlist = genTableColumnMapper.selectModelColumnByTableId(tableId);
            //拿到所有字段后去拼接创建sql的语句
            String sql = SqlUtils.createTableByHiveSql(genTable.getTableName(), genTable.getTableComment(), GenTableColumnlist);
            log.info(sql);
            try {
                hiveJdbcBaseDaoImpl.getJdbcTemplate().execute(sql);
            } catch (DataAccessException e) {
                log.error("创建表错误", e);
                return false;
            }
            return true;
        }
        return true;
    }

    /**
     * mysql 校验表是否存在
     *
     * @param tableName
     * @return
     */
    public boolean validateTableNameExist(String tableName) {
        //校验该表是否在数据库中存在
        String sql = "SELECT  count(*) FROM information_schema.TABLES WHERE table_name = '" + tableName + "' and TABLE_SCHEMA = '" + DbName + "' ;";
        int count = jdbcTemplate.queryForObject(sql, Integer.class);
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 在线收割处理
     *
     * @param dataAddToFtpVo
     * @return
     * @throws Exception
     */
    public Map<String, Object> setDataByFtp(DataAddToFtpVo dataAddToFtpVo) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 500);

        //文件名称
        String fileName = "";
        //最终完整地址 ,提供下载用
        String fileUrl = "";
        //文件名避免重复添加个时间戳
        String sjc = DateUtils.dateToStr_yyyyMMddHHMMss(new Date());
        //根据模型的id查模型总表信息
        GenTable genTable = genTableMapper.selectByPrimaryKey(Long.valueOf(dataAddToFtpVo.getTableId()));
        //根据模型id查询该模型下所有的字段数据
        List<GenTableColumn> GenTableColumnlist = genTableColumnMapper.selectModelColumnByTableId(dataAddToFtpVo.getTableId());

        if (null == genTable && GenTableColumnlist.isEmpty()) {
            throw new RuntimeException("模型数据为空,请稍后再试");
        }
        //开始连接输入的ftp服务
        SFTPUtil sftpUtil = new SFTPUtil(dataAddToFtpVo.getUsername(), dataAddToFtpVo.getPassword(), dataAddToFtpVo.getIp(), dataAddToFtpVo.getPort());
        ChannelSftp sftp = sftpUtil.login();
        //要收割的类型文件
        String dataType = dataAddToFtpVo.getDataType();
        //根据后缀来区分保存的路径,csv和json保存的地址不同
        String pathFile = "csv".equals(dataType) ? ftpCsvPath : "json".equals(dataType) ? ftpJsonPath : ftpCsvPath;//三目运算符
        //如果为空说明连接失败,账号密码错误等;
        if (null != sftp) {
            //文件路径:此处可能是目录也可能是指定的文件
            String path = dataAddToFtpVo.getPath();

            //说先判断单个文件还是整个目录
            if (path.contains(".")) {
                //先拿文件名
                int one = path.lastIndexOf("/");
                fileName = path.substring((one + 1), path.length());
                //文件名前面的路径
                String prefix = path.substring(0, path.lastIndexOf("/"));
                //后缀
                int one1 = path.lastIndexOf(".");
                String Suffix = path.substring((one1 + 1), path.length());

                //判断文件后缀和选中的类型是否一致
                if (!Suffix.equals(dataType)) {
                    throw new RuntimeException("您选中的数据类型与指定文件格式不同");
                }

                //判断填写的文件或者路径是否在服务器存在
                boolean isExist = sftpUtil.isExistsFile(prefix, fileName);
                if (!isExist) {
                    throw new RuntimeException("填写的文件或者目录地址不存在");
                }

                //先下载,在上传到服务器的hdfs
                fileUrl = pathFile + sjc + "-" + fileName;

                //将文件保存到本地
                sftpUtil.download(prefix, fileName, fileUrl);
                sftpUtil.logout();

                File jsonFile = new File(fileUrl);
                FileInputStream input = new FileInputStream(jsonFile);
                MultipartFile multipartFile = new MockMultipartFile("file", jsonFile.getName(), "text/plain", IOUtils.toByteArray(input));

                DataManualFileImportVo dataManualFile = new DataManualFileImportVo();
                dataManualFile.setModelId(dataAddToFtpVo.getTableId());
                dataManualFile.setImportName(dataAddToFtpVo.getModelTypeName());
                dataManualFile.setFileType(dataAddToFtpVo.getDataType());
                dataManualFile.setFile(multipartFile);
                dataManualFile.setDataMappingId(dataAddToFtpVo.getDataMappingId());
                dataManualFile.setDataMappingName(dataAddToFtpVo.getDataMappingName());
                dataManualFile.setProcessingMode(dataAddToFtpVo.getProcessingMode());

                map = fileProcessing(Constant.ONLINE, dataManualFile);
            } else {//目录操作
                String directoryPath = dataAddToFtpVo.getPath();
                //看目录是否存在
                boolean isExist = sftpUtil.isExistDir(directoryPath, sftp);
                if (!isExist) {
                    throw new RuntimeException("填写的目录不存在");
                }

                sftpUtil.cd(directoryPath);
                if (directoryPath != null && !"".equals(directoryPath)) {
                    sftp.cd(directoryPath);
                }
                //查询该目录下所有文件
                Vector<ChannelSftp.LsEntry> listFile = sftpUtil.listFiles(directoryPath);

                HashMap<String, String> mapUrl = new HashMap<>();
                List<String> list = new ArrayList<>();
                //遍历这些文件
                for (ChannelSftp.LsEntry ls : listFile) {
                    sjc = DateUtils.dateToStr_yyyyMMddHHMMss(new Date());
                    //依次拿到该目录下的文件名
                    String filename = ls.getFilename();
                    //查询目录下的格式文件
                    if (filename.contains("." + dataType)) {
                        fileUrl = TransferStation + sjc + "-" + filename;
                        //先下载后到本地目录下,在上传
                        sftpUtil.download(directoryPath, filename, fileUrl);
                        //将信息存map中
                        mapUrl.put(fileUrl, filename);
                        list.add(filename);
                    }
                }

                //开始把文件存放hdfs
                String hdfsUrl = putHdfsMoreFile(mapUrl);
                if ("-1".equals(hdfsUrl)) {
                    throw new RuntimeException("hdfs文件上传失败");
                }
                DataManualFileImportVo dataManualFile = new DataManualFileImportVo();
                dataManualFile.setModelId(dataAddToFtpVo.getTableId());
                dataManualFile.setImportName(dataAddToFtpVo.getModelTypeName());
                dataManualFile.setFileType(dataAddToFtpVo.getDataType());
                dataManualFile.setDataMappingId(dataAddToFtpVo.getDataMappingId());
                dataManualFile.setDataMappingName(dataAddToFtpVo.getDataMappingName());
                dataManualFile.setProcessingMode(dataAddToFtpVo.getProcessingMode());
                map = finalOperation(genTable, GenTableColumnlist, hdfsUrl, dataManualFile, list.toString(), Constant.ONLINE);

                map.put("code", 200);
                map.put("msg", "数据收割成功");
            }
        } else {
            map.put("msg", "fpt获取连接失败,请检查地址,账号密码等信息");
            log.error("fpt获取连接失败");
        }
        return map;


    }


    /**
     * 最后操作
     *
     * @param genTable
     * @param GenTableColumnlist
     * @param hdfsUrl
     * @param dataManualFile
     * @param fileName
     * @param type
     * @return
     */
    public Map<String, Object> finalOperation(GenTable genTable, List<GenTableColumn> GenTableColumnlist, String hdfsUrl, DataManualFileImportVo dataManualFile, String fileName, String type) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        String info = "";
        //判断表是否存在
        boolean rows = hiveService.validateTableNameExistByHive(genTable.getTableName());
        DataManage dataManage = new DataManage();
        //判断是手动导入还是在线收割
        if ("online".equals(type)) {
            dataManage.setSource("0");//数据来源(0代表收割，1代表手动导入)
            info = "数据收割任务提交成功";
        } else {
            dataManage.setSource("1");//数据来源(0代表收割，1代表手动导入)
            info = "数据导入任务提交成功";
        }
        //hive表是否存在
        boolean isno = rows ? true : false;
        //追加/覆盖/新建 ：0/1/2
        String types = isno ? "0".equals(dataManualFile.getProcessingMode()) ? "0" : "1" : "2";
        //需要调spark脚本执行
        //拼接参数
        JSONObject obj = createJSONObject(genTable.getTableName(), hdfsUrl, GenTableColumnlist,types, dataManualFile.getFileType());
        log.error("任务参数：" + obj.toString());
        //提交spark任务，返回applicationId
        String logFilePath = "/data/tobaccoZhongtai/spark/log/dataImport-" + DateUtils.dateToStr_YYYYMMDD(new Date()) + "-" + CommonUtils.getCode(8) + ".log";
        dataManage.setProgress(Task.WAITING);
        dataManage.setDataName(dataManualFile.getImportName());
        dataManage.setCreateTime(new Date());
        dataManage.setFileName(fileName);
        dataManage.setFilePath(hdfsUrl + "-" + fileName);
        dataManage.setTableId(genTable.getTableId());
        dataManage.setTableName(genTable.getTableName());
        dataManage.setStatus("0");
        dataManage.setDataType(dataManualFile.getFileType());
        dataManage.setFileSize("");
        dataManageMapper.insert(dataManage);

        File logFile = new File(logFilePath);
        try {
            logFile.createNewFile();
            //创建任务
            int taskId = createTask(genTable.getTableName(), SPARK_JAR_INPUT_DATA, obj.toString(), dataManage.getId(),"");

            Map<String, Object> jsonParam = new HashMap<>();
            jsonParam.put("dataMappingId", dataManualFile.getDataMappingId());
            jsonParam.put("dataMappingName", dataManualFile.getDataMappingName());
            jsonParam.put("operationMode", type);//操作方式
            jsonParam.put("hiveName", genTable.getTableName());
            jsonParam.put("tableId", genTable.getTableId());

            jsonParam.put("url", hdfsUrl + "-" + fileName);
            jsonParam.put("type", types);//新建,追加,覆盖
           String jsonStr = JSONObject.toJSONString(jsonParam);

            TaskListener taskListener = new TaskListener(String.valueOf(taskId), logFilePath, "", dataManage.getId(),jsonStr);
            //提交spark任务
            launcher.CreateSparkLauncher(SPARK_JAR_INPUT_DATA, obj.toString(), taskListener, logFile);
        } catch (IOException e) {
            log.error("spark任务提交异常" , e);
            info = "spark任务提交异常";
            result.put("msg", info);
            return result;
        }
        log.info("数据接入任务提交成功");
        result.put("code", 200);
        result.put("msg", info);
        return result;
    }


    /**
     * hdfs 文件上传
     *
     * @param type     类型（导入：offline或者接入：online）
     * @param fileUrl  文件的物理地址
     * @param fileName 文件名称
     */
    public String putHdfs(String type, String fileUrl, String fileName) {

        //判断类型是在线接入还是手工导入
        String hdfsPath = Constant.ONLINE.equals(type) ? hdfsOnlineFile : Constant.OFFLINE.equals(type) ? hdfsOfflineFile : "";
        try {
            if (!hdfsUtil.checkFileExist(hdfsPath)) {
                //再次判断文件目录是否存在,如果不在则新建
                hdfsUtil.mkdir(hdfsPath);
            }
            //新建一个时间目录,按年月日为group分类
            //获取今天的时间戳年月日
            String nowDate = DateUtils.dateToStr_YYYYMMDD(new Date());
            //因为通过hdfs建表并拆数据的话,需要用目录层,且目录下无其他文件,在此采用秒时间戳
            String dateToStr_yyyyMMddHHMMss = DateUtils.dateToStr_yyyyMMddHHMMss(new Date());

            if (!hdfsUtil.checkFileExist(hdfsPath + nowDate + "/" + dateToStr_yyyyMMddHHMMss)) {
                //再次判断文件目录是否存在,如果不在则新建
                hdfsUtil.mkdir(hdfsPath + nowDate + "/" + dateToStr_yyyyMMddHHMMss);
            }
            hdfsUtil.put(fileUrl, hdfsPath + nowDate + "/" + dateToStr_yyyyMMddHHMMss + "/" + fileName);
            return hdfsPath + nowDate + "/" + dateToStr_yyyyMMddHHMMss + "/";
        } catch (Exception e) {
            log.error(fileName + "put to hdfs error ",e);
        }
        return "-1";

    }

    /**
     * hdfs同目录多文件上传
     *
     * @param map
     * @return
     */
    public String putHdfsMoreFile(HashMap<String, String> map) {
        try {
            if (!hdfsUtil.checkFileExist(hdfsOnlineFile)) {
                //再次判断文件目录是否存在,如果不在则新建
                hdfsUtil.mkdir(hdfsOnlineFile);
            }

            //获取今天的时间戳年月日
            String nowDate = DateUtils.dateToStr_YYYYMMDD(new Date());
            //因为通过hdfs建表并拆数据的话,需要用目录层,且目录下无其他文件,在此采用秒时间戳
            String dateToStr_yyyyMMddHHMMss = DateUtils.dateToStr_yyyyMMddHHMMss(new Date());

            if (!hdfsUtil.checkFileExist(hdfsOnlineFile + nowDate + "/" + dateToStr_yyyyMMddHHMMss)) {
                //再次判断文件目录是否存在,如果不在则新建
                hdfsUtil.mkdir(hdfsOnlineFile + nowDate + "/" + dateToStr_yyyyMMddHHMMss);
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String mapKey = entry.getKey();
                String mapValue = entry.getValue();
                hdfsUtil.put(mapKey, hdfsOnlineFile + nowDate + "/" + dateToStr_yyyyMMddHHMMss + "/" + mapValue);
            }

            return hdfsOnlineFile + nowDate + "/" + dateToStr_yyyyMMddHHMMss + "/";
        } catch (Exception e) {
            log.error("put to hdfs error :",e);
        }
        return "-1";

    }

    public void changeDataImportProgress(Long id, String progress) {
        dataManageMapper.changeDataImportProgress(id, progress);
    }

    @Override
    public void setDataImportResult(Long id, String result) {
        dataManageMapper.setDataImportResult(id, result);
    }

    @Override
    public List<String> getAllTables(String userName, String password, String jdbcUrl) {
        DataBaseType dataBaseType = DataBaseType.retDataBaseType(jdbcUrl);
        Connection conn = DBUtil.getConnection(dataBaseType, jdbcUrl, userName, password, 1);
        String[] split = jdbcUrl.split("/");
        String dbName = split[split.length - 1];
        List<String> tables = DBUtil.getTables(dataBaseType, dbName, conn);
        return new ArrayList<>(tables);
    }

    @Override
    public void dataImportJDBC(String jdbcUrl, String userName, String password, String[] table, String importType, String modelTypeName) {

        //创建数据数据同步页面的展示任务
        DataManage dataManage = new DataManage();
        dataManage.setSource("0");
        dataManage.setProgress(Task.WAITING);
        dataManage.setDataName(modelTypeName);
        dataManage.setCreateTime(new Date());
        dataManage.setTableName(StringUtils.join(table, ","));
        dataManage.setStatus("0");
        dataManage.setDataType("jdbc");
        dataManage.setFileSize("");
        dataManageMapper.insert(dataManage);

        //这里直接创建异步任务  return
        /**
         * jdbcUrl userName password table
         */
        Map<String, Object> jsonParam = new HashMap<>();
        jsonParam.put("jdbcUrl", jdbcUrl);
        jsonParam.put("userName", userName);
        jsonParam.put("password", password);
        jsonParam.put("dataManageId", dataManage.getId());
        jsonParam.put("table", StringUtils.join(table, ","));
        jsonParam.put("importType", importType);

        Task task = new Task();
        task.setCreateTime(new Date());
        task.setTaskType(Task.ONLINE_IMPORT_DATA_BY_JDBC);
        task.setTaskName("JDBC-dataImport");
        task.setStatus(Task.WAITING);
        task.setJsonData(JSON.toJSONString(jsonParam));
        taskMapper.createTask(task);

    }

    @Override
    @Transactional
    public void dataImportJDBCNew(String jdbcUrl, String userName, String password,
                                  List<DataImportJDBCVo.Table> tables, String modelTypeName,
                                  String pid) {

        if (CollectionUtils.isEmpty(tables)){
            throw new RuntimeException("请选择需要导入的数据");
        }
        List<String> tableNames = tables.stream().map(ta -> ta.getTableName()).collect(Collectors.toList());
        String[] table = tableNames.toArray(new String[tableNames.size()]);
        table = table.clone();

        //遍历tables
        setHiveTableName(tables, pid);

        //创建数据数据同步页面的展示任务
        DataManage dataManage = new DataManage();
        dataManage.setSource("0");
        dataManage.setProgress(Task.WAITING);
        dataManage.setDataName(modelTypeName);
        dataManage.setCreateTime(new Date());
        dataManage.setTableName(StringUtils.join(table, ","));
        dataManage.setStatus("0");
        dataManage.setDataType("jdbc");
        dataManage.setFileSize("");
        dataManageMapper.insert(dataManage);

        //这里直接创建异步任务  return
        /**
         * jdbcUrl userName password table
         */
        Map<String, Object> jsonParam = new HashMap<>();
        jsonParam.put("jdbcUrl", jdbcUrl);
        jsonParam.put("userName", userName);
        jsonParam.put("password", password);
        jsonParam.put("dataManageId", dataManage.getId());
        jsonParam.put("tables", tables);
        jsonParam.put("pid", pid);

        Task task = new Task();
        task.setCreateTime(new Date());
        task.setTaskType(Task.ONLINE_IMPORT_DATA_BY_JDBC);
        task.setTaskName("JDBC-dataImport");
        task.setStatus(Task.WAITING);
        task.setJsonData(JSON.toJSONString(jsonParam));
        taskMapper.createTask(task);
    }

    @Override
    public void neo4jToHiveNew(DataImportJDBCVo dataImportJDBCVo) {

        if (CollectionUtils.isEmpty(dataImportJDBCVo.getLabels()) && CollectionUtils.isEmpty(dataImportJDBCVo.getRelations())) {
            throw new RuntimeException("请选择需要导入的数据table");
        }

        List<String> tableList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dataImportJDBCVo.getLabels())){
            List<String> labelNames = dataImportJDBCVo.getLabels().stream().map(label -> label.getTableName()).collect(Collectors.toList());
            tableList.addAll(labelNames);
        }
        if (!CollectionUtils.isEmpty(dataImportJDBCVo.getRelations())){
            List<String> relationNames = dataImportJDBCVo.getRelations().stream().map(relation -> relation.getTableName()).collect(Collectors.toList());
            tableList.addAll(relationNames);
        }
        String[] table = tableList.toArray(new String[tableList.size()]);
        table = table.clone();

        //遍历  如果导入类型为新建 需要创建 / 如果为覆盖  添加修改记录
        setHiveTableName(dataImportJDBCVo.getRelations(), dataImportJDBCVo.getPid());
        setHiveTableName(dataImportJDBCVo.getLabels(), dataImportJDBCVo.getPid());

        //创建数据数据同步页面的展示任务
        DataManage dataManage = new DataManage();
        dataManage.setSource("0");
        dataManage.setProgress(Task.WAITING);
        dataManage.setDataName(dataImportJDBCVo.getModelTypeName());
        dataManage.setCreateTime(new Date());
        dataManage.setTableName(StringUtils.join(table, ","));
        dataManage.setStatus("0");
        dataManage.setDataType("jdbc");
        dataManage.setFileSize("");
        dataManageMapper.insert(dataManage);

        //这里直接创建异步任务  return
        /**
         * jdbcUrl userName password table
         */
        Map<String, Object> jsonParam = new HashMap<>();
        jsonParam.put("boltUrl", dataImportJDBCVo.getBoltUrl());
        jsonParam.put("userName", dataImportJDBCVo.getUserName());
        jsonParam.put("password", dataImportJDBCVo.getPassword());
        jsonParam.put("dataManageId", dataManage.getId());
        jsonParam.put("pid", dataImportJDBCVo.getPid());
        jsonParam.put("labels", dataImportJDBCVo.getLabels());
        jsonParam.put("relations", dataImportJDBCVo.getRelations());

        Task task = new Task();
        task.setCreateTime(new Date());
        task.setTaskType(Task.NEO4J_TO_HIVE_TASK);
        task.setTaskName("JDBC-dataImport");
        task.setStatus(Task.WAITING);
        task.setJsonData(JSON.toJSONString(jsonParam));
        taskMapper.createTask(task);
    }

    /**
     * hive数据导入成功后 创建虚拟目录 model 血缘关系 修改记录
     * @param datas
     * @param pid
     * @param url
     */
    @Override
    public void doFinalWork(List<DataImportJDBCVo.Table> datas, String pid, String url){

        if (CollectionUtils.isEmpty(datas)) {
            return;
        }
        for (int i = 0; i < datas.size(); i++) {
            DataImportJDBCVo.Table temp = datas.get(i);
            if (temp.getImportType().equals("0")) { //追加
                //添加修改记录
                DataChangeLog dataChangeLog = new DataChangeLog();
                dataChangeLog.setTableName(temp.getHiveTableName());
                dataChangeLog.setOperationTime(new Date());
                dataChangeLog.setMode("追加数据");
                dataChangeLogService.insertDataChangeLog(dataChangeLog);

            } else if (temp.getImportType().equals("1")) { //覆盖
                //添加修改记录
                DataChangeLog dataChangeLog = new DataChangeLog();
                dataChangeLog.setTableName(temp.getHiveTableName());
                dataChangeLog.setOperationTime(new Date());
                dataChangeLog.setMode("覆盖数据");
                dataChangeLogService.insertDataChangeLog(dataChangeLog);
            } else { //新建

                //创建模型信息
                List<GenTableColumn> genTableColumns = new ArrayList<>(temp.getColumns().size());
                for (String columnName : temp.getColumns()) {
                    GenTableColumn column = new GenTableColumn();
                    column.setColumnName(columnName);
                    column.setColumnType("string");
                    genTableColumns.add(column);
                }
                GenTable genTable = new GenTable();
                genTable.setTableName(temp.getHiveTableName());
                genTable.setGenTableColumn(genTableColumns);
                genTableService.insertGenTable(genTable);

                //虚拟目录添加记录
                DataMapping dataMapping = new DataMapping();
                dataMapping.setCreateTime(new Date());
                dataMapping.setUpdateTime(new Date());
                dataMapping.setName(temp.getTableName());
                dataMapping.setTableName(temp.getHiveTableName());
                dataMapping.setParentId(Integer.valueOf(pid));
                dataMapping.setStatus("0");
                dataMapping.setModelId(genTable.getTableId().intValue());
                //类型为文件
                dataMapping.setType(1);
                dataMappingService.insert(dataMapping);

                //添加血缘关系 添加两个节点  一个是数据库 一个是新建的table
                NodeGraph source = NodeGraph.builder()
                        .table_name(url)
                        .build();
                NodeGraph target = NodeGraph.builder()
                        .table_name(temp.getHiveTableName())
                        .build();
                graphService.createNode(source);
                graphService.createNode(target);
                graphService.createRelationByTableName(url, temp.getHiveTableName());
            }
        }
    }

    //设置hiveTableName
    private void setHiveTableName(List<DataImportJDBCVo.Table> datas, String pid) {
        if (CollectionUtils.isEmpty(datas)) {
            return;
        }
        for (int i = 0; i < datas.size(); i++) {
            DataImportJDBCVo.Table temp = datas.get(i);
            if (temp.getImportType().equals("0")) { //追加
                //查询对应的hiveTableName
                String hiveTableName = dataMappingService.getHiveTableNameByPidAndName(Integer.valueOf(pid), temp.getTableName());
                //如果没有 抛出异常
                if (StringUtils.isEmpty(hiveTableName)) {
                    throw new RuntimeException(temp.getTableName() + "数据不可用, 没有找到对应映射");
                }
                //如果有的话 设置hiveTableName
                temp.setHiveTableName(hiveTableName);
            } else if (temp.getImportType().equals("1")) { //覆盖
                //查询对应的hiveTableName
                String hiveTableName = dataMappingService.getHiveTableNameByPidAndName(Integer.valueOf(pid), temp.getTableName());
                //如果没有 抛出异常
                if (StringUtils.isEmpty(hiveTableName)) {
                    throw new RuntimeException(temp.getTableName() + "数据不可用, 没有找到对应映射");
                }
                //如果有的话 设置hiveTableName
                temp.setHiveTableName(hiveTableName);
            } else { //新建
                //hiveTableName 自定义生成
                String hiveTableName = (temp.getTableName().contains("-") ? temp.getTableName().replace("-", "_") : temp.getTableName());
                hiveTableName = hiveTableName + Calendar.getInstance().getTimeInMillis();
                temp.setHiveTableName(hiveTableName);
            }
        }
    }

    @Override
    public void neo4jToHive(String modelTypeName, String boltUrl, String userName, String password, String tag) {

        //创建数据数据同步页面的展示任务
        DataManage dataManage = new DataManage();
        dataManage.setSource("0");
        dataManage.setProgress(Task.WAITING);
        dataManage.setDataName(modelTypeName);
        dataManage.setCreateTime(new Date());
        //dataManage.setFileName(sjc+"-"+fileName);
        //dataManage.setFilePath(hdfsUrl+"-"+fileName);
        //dataManage.setTableId(genTable.getTableId());
        //dataManage.setTableName(StringUtils.join(table, ","));
        dataManage.setStatus("0");
        dataManage.setDataType("jdbc");
        dataManage.setFileSize("");
        dataManageMapper.insert(dataManage);

        //这里直接创建异步任务  return
        /**
         * jdbcUrl userName password table
         */
        Map<String, Object> jsonParam = new HashMap<>();
        jsonParam.put("boltUrl", boltUrl);
        jsonParam.put("userName", userName);
        jsonParam.put("password", password);
        jsonParam.put("dataManageId", dataManage.getId());
        jsonParam.put("tag", tag);
        //jsonParam.put("table", StringUtils.join(table, ","));
        //jsonParam.put("importType", importType);

        Task task = new Task();
        task.setCreateTime(new Date());
        task.setTaskType(Task.NEO4J_TO_HIVE_TASK);
        task.setTaskName("JDBC-dataImport");
        task.setStatus(Task.WAITING);
        task.setJsonData(JSON.toJSONString(jsonParam));
        taskMapper.createTask(task);
    }


    @Override
    public void neo4jToHiveTest(String boltUrl, String userName, String password) {
        Session session = Neo4JUtil.getSession(boltUrl, userName, password);
        Neo4JUtil.closeSession(session);
    }

    @Override
    public Map<String, List<String>> getLabelsAndRelations(String boltUrl, String userName, String password) {
        Session session = null;
        try {
            Map<String, List<String>> ret = new HashMap<>();
            session = Neo4JUtil.getSession(boltUrl, userName, password);

            List<String> labels = Neo4JUtil.getLabels(session);
            List<String> relationshipTypes = Neo4JUtil.getRelationshipTypes(session);
            ret.put("labels", labels);
            ret.put("relations", relationshipTypes);
            return ret;
        } finally {
            if (session != null) {
                Neo4JUtil.closeSession(session);
            }
        }
    }


    /**
     * csv文件处理  目前已废弃
     *
     * @param type          类型（手动：offline或者收割：online）
     * @param model         选择的模型id
     * @param modelTypeName 接入名称
     * @param file          手动导入的文件,在线的传null
     * @param fileUrl       文件的物理地址 用来上传hdfs
     * @param fileName      文件名称
     * @return
     */
    public Map<String, Object> csvFileProcessing(String type, String model, String modelTypeName, String modelType, MultipartFile file, String fileUrl, String fileName) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        //文件名避免重复添加个时间戳
        String sjc = DateUtils.dateToStr_yyyyMMddHHMMss(new Date());
        //根据模型的id查模型总表信息
        GenTable genTable = genTableMapper.selectByPrimaryKey(Long.valueOf(model));
        //根据模型id查询该模型下所有的字段数据
        List<GenTableColumn> GenTableColumnlist = genTableColumnMapper.selectModelColumnByTableId(model);
        if (null == genTableMapper && GenTableColumnlist.isEmpty()) {
            result.put("msg", "模型数据为空,请稍后再试");
            return result;
        }


        if (Constant.OFFLINE.equals(type)) {

            //为了hdfs必须拿到文件的物理路径,但是因为各种限制,暂无办法获取，所以在传本地一次
            String fileUrls = TransferStation + sjc + "-" + fileName;
            //开始保存文件
            String upload = FileUtils.upload(file, fileUrls, "");

            //根据上传的返回值来校验上传情况
            Map<String, Object> mapUploadFile = JSON.parseObject(upload);
            String codes = (String) mapUploadFile.get("code");
            if ("500".equals(codes)) {
                result.put("msg", "文件上传失败,请稍后再试！");
                return result;
            }
            try {
                if (!hdfsUtil.checkFileExist(hdfsOfflineFile + "/" + genTable.getTableName())) {
                    //再次判断文件目录是否存在,如果不在则新建
                    hdfsUtil.mkdir(hdfsOfflineFile + "/" + genTable.getTableName());
                }
                hdfsUtil.put(fileUrl, hdfsOfflineFile + "/" + genTable.getTableName() + "/" + fileName);
            } catch (Exception e) {
                log.error("hdfs 创建目录失败:",e);
            }
        }
        //开始把文件存放hdfs
        String hdfsUrl = putHdfs(type, fileUrl, sjc + "-" + fileName);
        if ("-1".equals(hdfsUrl)) {
            result.put("msg", "hdfs文件上传失败");
            return result;
        }
        DataManualFileImportVo dataManualFile = new DataManualFileImportVo();
        dataManualFile.setModelId(modelType);
        dataManualFile.setImportName(modelTypeName);
        dataManualFile.setFileType(modelType);
        result = finalOperation(genTable, GenTableColumnlist, hdfsUrl, dataManualFile, file.getOriginalFilename(), type);
        return result;

    }

    public Map<String, Object> fileProcessing(String type, DataManualFileImportVo dataManualFile) {
        String fielName = dataManualFile.getFile().getOriginalFilename();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        //文件名避免重复添加个时间戳
        String sjc = DateUtils.dateToStr_yyyyMMddHHMMss(new Date());
        //根据模型的id查模型总表信息
        GenTable genTable = genTableMapper.selectByPrimaryKey(Long.valueOf(dataManualFile.getModelId()));
        //根据模型id查询该模型下所有的字段数据
        List<GenTableColumn> GenTableColumnlist = genTableColumnMapper.selectModelColumnByTableId(dataManualFile.getModelId());
        if (null == genTable && GenTableColumnlist.isEmpty()) {
            throw new RuntimeException("模型数据为空,请稍后再试");
        }
        //文件的物理地址，用来做hdfs上传
        String path = "";
        //json文件需要单独处理
        if ("json".equals(dataManualFile.getFileType())) {
            File files = FileUtils.multipartFileToFile(dataManualFile.getFile());
            path = TransferStation + sjc + "-" + fielName;
            FileUtils.readJsonFileAndWrite(files, path);
        } else {
            String fileUrls = TransferStation + sjc + "-";
            //开始保存文件
            String upload = FileUtils.upload(dataManualFile.getFile(), fileUrls, "");
            //根据上传的返回值来校验上传情况
            Map<String, Object> mapUploadFile = JSON.parseObject(upload);
            String codes = (String) mapUploadFile.get("code");
            if ("500".equals(codes)) {
                throw  new RuntimeException("文件上传失败,请稍后再试！");
            }
            path = (String) mapUploadFile.get("url");
        }

        //开始把文件存放hdfs
        String hdfsUrl = putHdfs(type, path, sjc + "-" + fielName);
        if ("-1".equals(hdfsUrl)) {
            result.put("msg", "hdfs文件上传失败");
            return result;
        }
        result = finalOperation(genTable, GenTableColumnlist, hdfsUrl, dataManualFile, sjc + "-" + fielName, type);
        return result;
    }


    //目前已废弃
    public Map<String, Object> offlineCsvFileProcessing(String type, String model, String modelTypeName, String modelType, String fileUrl, String fileName) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        //判断类型是在线接入还是手工导入
        String hdfsPath = "online".equals(type) ? hdfsOnlineFile : "offline".equals(type) ? hdfsOfflineFile : "";
        //文件名避免重复添加个时间戳
        String sjc = DateUtils.dateToStr_yyyyMMddHHMMss(new Date());
        //根据模型的id查模型总表信息
        GenTable genTable = genTableMapper.selectByPrimaryKey(Long.valueOf(model));
        //根据模型id查询该模型下所有的字段数据
        List<GenTableColumn> GenTableColumnlist = genTableColumnMapper.selectModelColumnByTableId(model);
        if (null == genTable && GenTableColumnlist.isEmpty()) {
            log.error("模型数据为空,请稍后再试；id：" + model);
            result.put("msg", "模型数据为空,请稍后再试");
            return result;
        }

        //判断表是否存在
        boolean rows = hiveService.validateTableNameExistByHive(genTable.getTableName());
        if (!rows) {
            String sql = sqlUtils.createTableToHiveByHdfs(genTable.getTableName(), genTable.getTableComment(), GenTableColumnlist, hdfsPath + "/" + genTable.getTableName());
            log.info(sql);
            try {
                hiveJdbcBaseDaoImpl.getJdbcTemplate().execute(sql);
            } catch (DataAccessException e) {
                log.error("hive创建表错误", e);
                result.put("msg", "模型数据为空,请稍后再试");
                return result;
            }
        }

        try {
            if (!hdfsUtil.checkFileExist(hdfsPath + "/" + genTable.getTableName())) {
                //再次判断文件目录是否存在,如果不在则新建
                hdfsUtil.mkdir(hdfsPath + "/" + genTable.getTableName());
            }
            hdfsUtil.put(fileUrl, hdfsPath + "/" + genTable.getTableName() + "/" + fileName);
        } catch (Exception e) {
            log.error("csv 文件上传hdfs异常" + e);
            result.put("msg", "文件上传hdfs异常,请稍后再试");
            return result;
        }
        DataManage dataManage = new DataManage();
        //判断是手动导入还是在线收割
        dataManage.setSource(Constant.ONLINE.equals(type) ? "0" : "1");//数据来源(0代表收割，1代表手动导入)
        String msg = "online".equals(type) ? "数据收割完成" : "数据导入完成";

        dataManage.setProgress(Task.DONE);
        dataManage.setDataName(modelTypeName);
        dataManage.setCreateTime(new Date());
        dataManage.setFileName(sjc + "-" + fileName);
        dataManage.setFilePath(hdfsPath + "/" + genTable.getTableName() + "/" + fileName);
        dataManage.setTableId(genTable.getTableId());
        dataManage.setTableName(genTable.getTableName());
        dataManage.setStatus("0");
        dataManage.setDataType(modelType);
        dataManage.setFileSize("");
        dataManageMapper.insert(dataManage);
        log.info("csv文件导入完成");
        result.put("code", 200);
        result.put("msg", msg);

        return result;

    }



    @Override
    public Map<String,Object> httpToData(HttptoDataVo httptoDataVo){
        Map<String, Object> map = new HashMap<>();
        map.put("code", 500);
        // 参数准备并处理url后面跟参数的情况
        Map<String, Object> Urlparams = HttpsUtils.urlAndParamSplit(httptoDataVo.getUrl(), "url");
        //参数统一处理
        Map<String, Object> params = httpParamToMap(httptoDataVo.getParms_tab(), httptoDataVo);
        params.putAll(Urlparams);

        //接口返回值
        String result = "";
        if (Constant.POST.equals(httptoDataVo.getType())){
            result = HttpsUtils.doPost(httptoDataVo.getHts()+httptoDataVo.getUrl(), params,httptoDataVo.getCharset(),  false);
        }else if (Constant.GET.equals(httptoDataVo.getType())){
             result = HttpsUtils.doGet(httptoDataVo.getHts()+httptoDataVo.getUrl(), HttpsUtils.getUrlParamsByMap(params),httptoDataVo.getCharset(),  false);
        }else{
            throw new RuntimeException("暂不支持post和get以外的请求方式！");
        }
        //返回结果转JSONObject
        net.sf.json.JSONObject obj = net.sf.json.JSONObject.fromObject(result);
        log.info(httptoDataVo.getHts()+ httptoDataVo.getUrl()+"接口返回结果："+result);
//        if ("500".equals(obj.getString("code")) || StringUtils.isBlank(result)) {
//            log.error("接口返回数据异常...");
//            throw new RuntimeException("接口返回数据异常...");
//        }
        //接口测试返回结果
        if ("test".equals(httptoDataVo.getOperationMode())) {
            log.info("http接口请求结果返回成功...");
            map.put("code", 200);
            map.put("msg", "http接口请求结果返回成功");
            map.put("data", result);
            return map;
        }
        //返回编码
        String code = obj.getString("code");
        //返回数据
        String data = obj.getString(httptoDataVo.getDataTitleParam());
        //返回tableName
        String name = obj.getString("name");
        if(!code.equals("200") || StringUtils.isBlank(data)){
            throw new RuntimeException("接口返回数据异常");
        }
        //开始准备处理返回数据
        //首先拿到字段数据
        JSONArray column =  gethttpColumnByData(data);
        //把data中的数据内容转存为json文件,再通过json文件方式调用spark任务进行入库
        String sjc = DateUtils.dateToStr_yyyyMMddHHMMss(new Date());
        //json存在本地物理路径
        String path = TransferStation + sjc + "-" + name+".json";

        //追加/覆盖/新建 ：0/1/2
        boolean rows = false;
        DataMapping dataMapping = dataMappingService.getById(httptoDataVo.getPid());
        if(null != dataMapping && StringUtils.isNotBlank(dataMapping.getTableName())){
            //判断虚拟目录是否已有表存在
              rows = hiveService.validateTableNameExistByHive(dataMapping.getTableName());
        }
        //目前就追加和新建
        String type = rows ? "0" : "2";
        String hiveName =  rows ? dataMapping.getTableName() : name+"_"+CommonUtils.getCode(4);

        try {
            FileUtils.writeJsonToFile(data, path);
            log.info("返回结果已保存到文件："+path);
            //保存完成后开始把文件存放hdfs
            String hdfsUrl = putHdfs(Constant.ONLINE, path, sjc + "-" + name+".json");
            if ("-1".equals(hdfsUrl)) {
                throw new RuntimeException("hdfs文件上传失败");
            }

            //准备调用spark参数
            JSONObject paramToSpark = httpDataToSparkparam(hiveName, hdfsUrl, column);
            //提交spark任务，返回applicationId
            String logFilePath = "/data/tobaccoZhongtai/spark/log/dataImport-" + DateUtils.dateToStr_YYYYMMDD(new Date()) + "-" + CommonUtils.getCode(8) + ".log";
            DataManage dataManage = new DataManage();
            dataManage.setSource("0");//数据来源(0代表收割，1代表手动导入)
            dataManage.setProgress(Task.WAITING);
            dataManage.setDataName(httptoDataVo.getImportName());
            dataManage.setCreateTime(new Date());
            dataManage.setFileName(sjc + "-" + name+".json");
            dataManage.setFilePath(hdfsUrl + "-" + sjc + "-" + name+".json");
          //  dataManage.setTableId(genTable.getTableId());
            dataManage.setTableName(name);
            dataManage.setStatus("0");
            dataManage.setDataType("http");
            dataManage.setFileSize("");
            dataManageMapper.insert(dataManage);


            File logFile = new File(logFilePath);
            try {
                logFile.createNewFile();
                //创建任务
                int taskId = createTask(name, SPARK_JAR_INPUT_DATA, paramToSpark.toString(), dataManage.getId(),"http");
                Map<String, Object> jsonParam = new HashMap<>();
                jsonParam.put("url",httptoDataVo.getHts()+ httptoDataVo.getUrl());
                jsonParam.put("dataMappingId", httptoDataVo.getPid());
                jsonParam.put("dataMappingName", httptoDataVo.getDataMappingName());
                jsonParam.put("operationMode", "http");//操作方式
                jsonParam.put("hiveName", hiveName);
                jsonParam.put("column", column);
                jsonParam.put("type",type);//新建,追加,覆盖

                TaskListener taskListener = new TaskListener(String.valueOf(taskId), logFilePath, "", dataManage.getId(),JSON.toJSONString(jsonParam));
                //提交spark任务
                launcher.CreateSparkLauncher(SPARK_JAR_INPUT_DATA, paramToSpark.toString(), taskListener, logFile);
                log.info("提交spark任务成功");
            } catch (IOException e) {
                log.error("spark任务提交异常" , e);
                map.put("msg", "spark任务提交异常");
                return map;
            }
            log.info("http数据接入任务提交成功");
            map.put("code", 200);
            map.put("msg", "http数据接入方式任务提交成功");
            map.put("data", result);

        }catch (Exception e) {
            log.error("接口返回结果处理异常",e);
            map.put("msg","接口返回结果处理异常");
        }
        return map;
    }

    /**
     * http方式调用后通过数据内容拿到column信息
     * @param data
     * @return
     */
    private JSONArray gethttpColumnByData(String data){
        JSONArray column = new JSONArray();
        //转换json格式
        JSONArray  array = JSONArray.parseArray(data);
        if(null != array && array.size()>0){
                com.alibaba.fastjson.JSONObject arr = (com.alibaba.fastjson.JSONObject) array.get(0);
                Iterator it =arr.entrySet().iterator();
                int x = 0;
                while (it.hasNext()) {
                    Map.Entry<String, Object> entry = (Map.Entry<String, Object>) it.next();
                    column.add(x++,entry.getKey());
                }
        }
        return column;
    }


    /**
     * 处理 http 返回数据后调用spark的参数
     * @param name
     * @param hdfsUrl
     * @param array
     * @return
     */
    private JSONObject  httpDataToSparkparam (String name,String hdfsUrl,JSONArray array){
        JSONObject result = new JSONObject();
        JSONObject obj = new JSONObject();
        JSONArray json = new JSONArray();

        //判断表是否存在
        boolean rows = hiveService.validateTableNameExistByHive(name);

        obj.put("hiveTableName", name);//hive 表名
      //  obj.put("hvieTableName", name+"_"+CommonUtils.getCode(4));//hive 表名
        obj.put("hdfs", hdfsUrl);//hdfs路径
        //追加/覆盖/新建 ：0/1/2
        //目前就追加和新建
        if(rows){
                obj.put("type", "0");
        }else{
            obj.put("type", "2");
        }
        obj.put("suffix", "json");//文件格式
        obj.put("column", array);//字段
        obj.put("sign", ",");//隔离符
        json.add(obj);
        result.put("tables", json);//库.表名
        log.info("http返回内容处理后调用脚本：" + result.toString());
        return result;
    }

    /**
     * 参数处理为map ：kv / name=sojson&domain=www.sojson.com /name=sojson&domain=www.sojson.com
     * @param parms_tab 参数的类型
     * @param httptoDataVo
     * @return
     */
    private Map<String, Object> httpParamToMap(String parms_tab,HttptoDataVo httptoDataVo){
        Map<String, Object> mapRequest = new HashMap<String, Object>();
        if ("tab_kv".equals(parms_tab)){
            List<HttptoDataVo.paramData> kvParms = httptoDataVo.getKvParms();
            for (HttptoDataVo.paramData kv:kvParms) {
                if (StringUtils.isNotBlank(kv.getKey()) && StringUtils.isNotBlank(kv.getValue())){
                    mapRequest.put(kv.getKey(),kv.getValue());
                }
            }
        }else if ("tab_batch".equals(parms_tab)){
           String batchParms = httptoDataVo.getBatchParms();
            mapRequest = HttpsUtils.urlAndParamSplit(batchParms,"param");
        }else if ("tab_json".equals(parms_tab)){
            String batchJson = httptoDataVo.getBatchParms();
            mapRequest = (Map)JSON.parse(batchJson);
        }
        return mapRequest;
    }

    /**
     * hive数据导入成功后 创建虚拟目录 model 血缘关系 修改记录
     * @param jsonParam
     */
    @Override
    public void createModelAndMapping(String jsonParam) {
        log.info("hive数据导入成功后 创建虚拟目录 model 血缘关系 修改记录。。。");
        if (StringUtils.isBlank(jsonParam) || "".equals(jsonParam)) {
            return;
        }
        String sjc = DateUtils.dateToStr_yyyyMMddHHMMss(new Date());
        JSONObject param = JSON.parseObject(jsonParam);
        //虚拟目录的id
        String  dataMappingId = param.getString("dataMappingId");
        //虚拟目录的名称
        String dataMappingName = param.getString("dataMappingName");
        DataMapping dataMapping = dataMappingService.getById(Integer.parseInt(dataMappingId));

        String type = param.getString("type");//新建,追加,覆盖
        String hiveName = param.getString("hiveName");//表名
        String operationMode = param.getString("operationMode");
        JSONArray column = (JSONArray) param.get("column");//字段
        GenTable genTable = new GenTable();
        if ("http".equals(operationMode)) {
            //模型和字段信息
            //创建模型信息
            List<GenTableColumn> genTableColumns = new ArrayList<>(column.size());
            for (int i = 0; i < column.size(); i++) {
                GenTableColumn c = new GenTableColumn();
                c.setColumnName((String) column.get(i));
                c.setColumnType("string");
                c.setCreateTime(new Date());
                genTableColumns.add(c);
            }
            genTable.setTableName(hiveName);
            genTable.setGenTableColumn(genTableColumns);
            genTableService.insertGenTable(genTable);
            log.info("http方式任务完成,且模型创建成功");

        } else if ("clear".equals(operationMode) || "participle".equals(operationMode)) {

            String tableName = param.getString("table");
            String newTableName = param.getString("newTableName");
            if(StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(newTableName) ){

                //创建模型信息
                List<GenTableColumn> genTableColumns = new ArrayList<>(column.size());
                for (int i = 0; i < column.size(); i++) {
                    GenTableColumn c = new GenTableColumn();
                    c.setColumnName((String) column.get(i));
                    c.setColumnType("string");
                    c.setCreateTime(new Date());
                    genTableColumns.add(c);
                }
                genTable.setTableName(newTableName);
                genTable.setGenTableColumn(genTableColumns);
                genTableService.insertGenTable(genTable);
                log.info("分词和创建中间表方式任务完成,且模型创建成功");

                //添加血缘关系 添加两个节点  一个是数据库 一个是新建的table
                NodeGraph source = NodeGraph.builder()
                        .table_name(tableName)
                        .build();
                NodeGraph target = NodeGraph.builder()
                        .table_name(newTableName)
                        .build();
                graphService.createNode(source);
                graphService.createNode(target);
                graphService.createRelationByTableName(tableName, newTableName);

                //文件夹的话创建新的虚拟文件和hiveTable 进行绑定
                DataMapping dm = new DataMapping();
                dm.setCreateTime(new Date());
                dm.setUpdateTime(new Date());
                dm.setName(dataMappingName + "-" + CommonUtils.getCode(4));
                dm.setTableName(newTableName);
                dm.setParentId(dataMapping.getParentId());
                dm.setStatus("0");
                //类型默认为文件
                dm.setType(1);
                dm.setModelId(genTable.getTableId().intValue());
                dataMappingService.insert(dm);
                return;
            } else if ("participle".equals(operationMode)) {
                //分词覆盖情况添加新加列给模型
                GenTable genTableVo =  genTableMapper.selectGenTableBytableName(tableName);
                if(null != genTableVo){
                    String newColumnName = param.getString("newColumnName");
                    GenTableColumn c = new GenTableColumn();
                    c.setColumnName(newColumnName);
                    c.setColumnType("string");
                    c.setCreateTime(new Date());
                    c.setTableId(String.valueOf(genTableVo.getTableId()));
                    genTableColumnMapper.insert(c);
                    return;
                }

            }
        }

        //虚拟目录添加记录

        //类型(0代表文件夹,1代表表)
        if (0 == dataMapping.getType()) {
            //文件夹的话创建新的虚拟文件和hiveTable 进行绑定
            DataMapping dm = new DataMapping();
            dm.setCreateTime(new Date());
            dm.setUpdateTime(new Date());
            dm.setName(dataMappingName + "-" + sjc);
            dm.setTableName(hiveName);
            dm.setParentId(Integer.parseInt(dataMappingId));
            dm.setStatus("0");
            //类型默认为文件
            dm.setType(1);
            if(null == genTable || null == genTable.getTableId()){
                String  tableId = param.getString("tableId");
                dm.setModelId(Integer.parseInt(tableId));
            }else{
                dm.setModelId(genTable.getTableId().intValue());
            }
            dataMappingService.insert(dm);
        } else {
            //如果是文件的话直接修改
            DataMapping dmup = new DataMapping();
            dmup.setUpdateTime(new Date());
            dmup.setTableName(hiveName);
            dmup.setId(Integer.parseInt(dataMappingId));
            dataMappingService.update(dmup);
        }

        //添加血缘关系 添加两个节点  一个是数据库 一个是新建的table
        NodeGraph source = NodeGraph.builder()
                .table_name(param.getString("url"))
                .build();
        NodeGraph target = NodeGraph.builder()
                .table_name(hiveName)
                .build();
        graphService.createNode(source);
        graphService.createNode(target);
        graphService.createRelationByTableName(param.getString("url"), hiveName);
        //日志记录
        //追加/覆盖/新建 ：0/1/2
        if ("0".equals(type)) { //追加
            //添加修改记录
            DataChangeLog dataChangeLog = new DataChangeLog();
            dataChangeLog.setTableName(hiveName);
            dataChangeLog.setOperationTime(new Date());
            dataChangeLog.setMode(operationMode + "方式追加数据");
            dataChangeLogService.insertDataChangeLog(dataChangeLog);

        } else if ("1".equals(type)) { //覆盖
            //添加修改记录
            DataChangeLog dataChangeLog = new DataChangeLog();
            dataChangeLog.setTableName(hiveName);
            dataChangeLog.setOperationTime(new Date());
            dataChangeLog.setMode(operationMode + "方式覆盖数据");
            dataChangeLogService.insertDataChangeLog(dataChangeLog);
        }
        log.info("虚拟目录映射,模型,血缘,操作日志记录已经创建完成...");
    }

}
