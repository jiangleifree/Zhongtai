package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.system.config.Launcher;
import cn.cnic.zhongtai.system.dao.SqlDao;
import cn.cnic.zhongtai.system.listener.TaskListener;
import cn.cnic.zhongtai.system.mapper.TaskMapper;
import cn.cnic.zhongtai.system.mapper.TopicInterfaceMapper;
import cn.cnic.zhongtai.system.mapper.TopicMapper;
import cn.cnic.zhongtai.system.model.*;
import cn.cnic.zhongtai.system.model.vo.GenTableColumnVO;
import cn.cnic.zhongtai.system.model.vo.GenTableVO;
import cn.cnic.zhongtai.system.service.*;
import cn.cnic.zhongtai.utils.CommonUtils;
import cn.cnic.zhongtai.utils.DBUtil;
import cn.cnic.zhongtai.utils.DataBaseType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.cnic.zhongtai.system.Constant.TO_MYSQL_JAR;
import static cn.cnic.zhongtai.system.Constant.TopicConstant.TOPIC_URL_PRE;

@Service
@Transactional
@Slf4j
public class TopicServiceImpl implements TopicService {

    @Resource
    private TopicMapper topicMapper;
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private MysqlSchemaService mysqlSchemaService;
    @Resource
    private TopicInterfaceMapper topicInterfaceMapper;
    @Resource
    private TableInfoService tableInfoService;
    @Resource
    private Launcher launcher;
    @Resource
    private SqlDaoService sqlDaoService;
    @Resource
    private SqlDao sqlDao;
    @Resource
    private InterfaceParamService interfaceParamService;

    @Override
    public List<TopicRepository> getAllTopic() {
        return topicMapper.getAllTopic();
    }

    @Override
    @Transactional
    public void createTopic(TopicRepository topicRepository) {

        //定义主题库对应的dbName  db + UUID
        String dbName = "db" + CommonUtils.getUUID32();
        topicRepository.setDbName(dbName);
        topicRepository.setCreateTime(new Date());
        //mysql创建DB
        //boolean isDBCreated = sqlDao.createDatabase(dbName);
        boolean isDBCreated = sqlDaoService.createDB(dbName);
        //创建topic对象
        topicMapper.createTopic(topicRepository);
        try {
            if (isDBCreated) { //db创建成功
                List<GenTableVO> tables = topicRepository.getTables();


                //首先判断是否有table数据 没有直接返回
                if (CollectionUtils.isEmpty(tables)) {
                    throw new RuntimeException("创建topic失败,tables must not be empty");
                }

                for (int i = 0; i < tables.size(); i++) {  //创建主题库中包含的所有table
                    //拼接查询条件
                    StringBuilder hql = new StringBuilder();
                    hql.append("select ");

                    GenTableVO table = tables.get(i);
                    List<GenTableColumnVO> columns = table.getGenTableColumn();
                    if (CollectionUtils.isEmpty(columns)) {
                        throw new RuntimeException("创建topic失败,column参数有误");
                    }

                    //重新定义table的列名
                    for (int j = 0; j < columns.size(); j++) {
                        //String realColumnName = columns.get(j).getFromTable() + "_" + columns.get(j).getColumnName();
                        //columns.get(j).setRealColumnName(realColumnName);
                        if (j == columns.size() - 1) {
                            hql.append(columns.get(j).getFromTable() + "." + "@" + columns.get(j).getColumnName() + "@ as @" + columns.get(j).getColumnName() + "@ ");
                        } else {
                            hql.append(columns.get(j).getFromTable() + "." + "@" + columns.get(j).getColumnName() + "@ as @" + columns.get(j).getColumnName() + "@, ");
                        }
                    }
                    //添加关联条件
                    hql.append(" " + table.getAssociatedConditions());
                    //添加where条件
                    if (!StringUtils.isEmpty(table.getWhere())) {
                        hql.append(" where " + table.getWhere());
                    }
                    //mysql创建table
                    //sqlDao.createTable(dbName, table);
                    boolean isCreated = sqlDaoService.createTable(dbName, table);
                    if (!isCreated) {
                        throw new RuntimeException(table.getTableName() + " 创建失败");
                    }
                    //mysql创建对应对外提供服务的接口
                    //createTopicInterface(topicRepository);

                    //提交spark任务，返回applicationId
                    String logFilePath = "/data/tobaccoZhongtai/spark/log/zhongtai" + CommonUtils.getUUID32() + ".log";
                    Map<String, String> sparkParam = new HashMap<>();
                    sparkParam.put("mysqlDBName", dbName);
                    sparkParam.put("mysqlTableName", table.getTableName());
                    sparkParam.put("hiveSql", hql.toString());
                    sparkParam.put("url", Constant.MYSQL_URL);

                    //创建任务
                    int taskId = createTask("", logFilePath);
                    File logFile = new File(logFilePath);
                    logFile.createNewFile();
                    TaskListener taskListener = new TaskListener(String.valueOf(taskId), logFilePath, "", null, "");
                    String args = JSON.toJSONString(sparkParam);

                    //提交spark任务
                    launcher.CreateSparkLauncher(TO_MYSQL_JAR, args, taskListener, logFile);

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            //rollback
            //sqlDao.deleteDatabaseByDBName(dbName);
            sqlDaoService.deleteDB(dbName);
            throw new RuntimeException(e.getMessage());
        }

    }

    /**
     * 创建task
     *
     * @param applicationId
     * @param logFilePath
     */
    public int createTask(String applicationId, String logFilePath) {
        Map<String, String> jsonParam = new HashMap<>();
        jsonParam.put("applicationId", applicationId);
        jsonParam.put("logFilePath", logFilePath);

        Task task = new Task();
        task.setCreateTime(new Date());
        task.setTaskType(Task.TopicTask);
        task.setTaskName(Task.TopicTask);
        task.setStatus(Task.WAITING);
        task.setJsonData(JSON.toJSONString(jsonParam));
        taskMapper.createTask(task);
        return task.getTaskId();
    }

    /**
     * 创建该table对外提供的接口
     */
    public void createTopicInterface(TopicRepository topicRepository) {
        TopicInterface topicInterface = new TopicInterface();
        topicInterface.setTopicId(Integer.valueOf(topicRepository.getTopicId()));
        topicInterface.setTopicName(topicRepository.getTopicName());
        topicInterface.setType("POST");
        topicInterface.setUrl(TOPIC_URL_PRE + topicRepository.getTopicName() + "/getList");
        topicInterfaceMapper.create(topicInterface);
    }


    @Override
    public TopicRepository findTopicByTopicName(String topicName) {
        return topicMapper.findTopicByTopicName(topicName);
    }

    @Override
    @Transactional
    public void deleteTopic(String topicName) {
        String dbName = topicMapper.getDBNameByTopicName(topicName);
        if (dbName == null) {
            throw new RuntimeException("topic is not exist");
        }
        //删除主题库对应的db
        sqlDao.deleteDatabaseByDBName(dbName);
        //删除数据库中topic信息
        topicMapper.deleteTopicByTopicName(topicName);
        //删除对应的接口信息
        topicInterfaceMapper.deleteByTopicName(topicName);
    }

    @Override
    public List<Map<String, Object>> getAllDataByTableName(String topicName, String tableName) {
        String dbName = topicMapper.getDBNameByTopicName(topicName);
        if (dbName == null) {
            throw new RuntimeException("topic is not exist");
        }
        return sqlDao.getAllDataByTableName(dbName, tableName);
    }

    @Override
    public List<String> getAllFieldsByDBNameAndTableName(String topicName, String tableName) {

        String dbName = topicMapper.getDBNameByTopicName(topicName);
        if (StringUtils.isEmpty(dbName)) {
            throw new RuntimeException("topicName is not exist");
        }
        return mysqlSchemaService.getAllFieldsByDBNameAndTableName(dbName, tableName);
    }

    @Override
    public TableInfo getTableInfoByTopicAndTableName(String topicName, String tableName) {
        String dbName = topicMapper.getDBNameByTopicName(topicName);
        if (StringUtils.isEmpty(dbName)) {
            throw new RuntimeException("topicName is not exist");
        }
        TableInfo tableInfo = mysqlSchemaService.getTableInfoByDBNameAndTableName(dbName, tableName);
        return tableInfo;
    }

    @Override
    public boolean authentication(String topicName, String userName, String password) {
        return !(topicMapper.selectTopicByPwd(topicName, userName, password) == null);
    }

    @Override
    public int getTotalCount() {
        return topicMapper.getTotalCount();
    }

    @Override
    public List<Map<String, String>> testSql(String topicName, String sql) {
        String dbName = topicMapper.getDBNameByTopicName(topicName);
        if (StringUtils.isEmpty(dbName)) {
            throw new RuntimeException("param error");
        }
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3307/" + dbName;
        Connection connection = DBUtil.getConnection(DataBaseType.MySql, jdbcUrl, "root", "xxx", 1);
        List<Map<String, String>> retData = new ArrayList<>();
        try {
            ResultSet resultSet = DBUtil.query(connection, sql, 1);
            int columnCount = resultSet.getMetaData().getColumnCount();
            while (resultSet.next()) {
                Map<String, String> data = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String colName = resultSet.getMetaData().getColumnName(i);
                    String colValue = resultSet.getString(colName);
                    data.put(colName, colValue);
                }
                retData.add(data);
            }
            return retData;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("sql error");
        }

    }


    @Override
    public void createInterface(InterfaceParam param) {

        InterfaceParam byUrl = interfaceParamService.getByUrl(param.getUrl());
        if (byUrl != null) {
            throw new RuntimeException("url 已经存在, 请修改后重新创建");
        }

        param.setCreateDate(new Date());
        param.setBelong("Topic");
        param.setConsumes("application/json");
        param.setProduces("*/*");

        JSONArray parameters = new JSONArray();
        parameters.addAll(param.getParameterList());
        JSONArray responses = new JSONArray();
        responses.addAll(param.getResponseList());

        param.setParametersJsonStr(parameters.toJSONString());
        param.setResponsesJsonStr(responses.toJSONString());

        interfaceParamService.insert(param);
    }

    @Override
    public List<String> getAllFieldsByTopicName(String topicName) {
        String dbName = topicMapper.getDBNameByTopicName(topicName);
        List<String> ret = new ArrayList<>();
        if (StringUtils.isEmpty(dbName)) {
            throw new RuntimeException("db error");
        }
        List<TableInfo> tableInfoList
                = mysqlSchemaService.getAllFieldsByDBName(dbName);
        for (TableInfo tableInfo : tableInfoList) {
            for (ColumnSimpleInfo columnSimpleInfo : tableInfo.getColumnSampleInfos()) {
                ret.add("" + tableInfo.getTableName() + "." + columnSimpleInfo.getColumnName());
            }
        }
        return ret;
    }

    @Override
    public Map<String, Object> getTopicInterfaceInfo(String topicName) {

        Map<String, Object> ret = new HashMap<>(3);

        TopicRepository topicRepository = topicMapper.getByTopicName(topicName);
        List<InterfaceParam> interfaceParams = interfaceParamService.getListByTopicName(topicName);
        List<String> tables = tableInfoService.findTablesByTopicName(topicName)
                .stream().map(tableInfo -> tableInfo.getTableName()).collect(Collectors.toList());
        ret.put("tables", tables);

        Map<String, Object> info = new HashMap<>(4);
        info.put("topicName", topicRepository.getTopicName());
        info.put("createTime", topicRepository.getCreateTime());
        info.put("comment", topicRepository.getComment());
        info.put("interfaceCount", interfaceParams.size());
        ret.put("info", info);

        List<Map<String, Object>> paths = new ArrayList<>();
        Map<String, Object> path = null;
        for (int i = 0; i < interfaceParams.size(); i++) {
            path = new HashMap<>();
            InterfaceParam param = interfaceParams.get(i);
            List<InterfaceParam.Parameter> parameters = JSONArray.parseArray(param.getParametersJsonStr(), InterfaceParam.Parameter.class);
            List<InterfaceParam.Parameter> responses = JSONArray.parseArray(param.getResponsesJsonStr(), InterfaceParam.Parameter.class);
            path.put("url", param.getUrl());
            path.put("type", param.getType());
            path.put("consumes", param.getConsumes());
            path.put("produces", param.getProduces());
            path.put("summary", param.getSummary());
            path.put("id", param.getId());
            path.put("parameters", parameters);
            path.put("responses", responses);
            paths.add(path);
        }
        ret.put("paths", paths);

        return ret;
    }
}
