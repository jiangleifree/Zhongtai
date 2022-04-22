package cn.cnic.zhongtai.system.service.Impl;
import	java.util.ArrayList;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.system.config.Launcher;
import cn.cnic.zhongtai.system.dao.HiveDao;
import cn.cnic.zhongtai.system.listener.StatisticalInformationListener;
import cn.cnic.zhongtai.system.listener.TaskListener;
import cn.cnic.zhongtai.system.mapper.DataChangeLogMapper;
import cn.cnic.zhongtai.system.mapper.ParticipleMapper;
import cn.cnic.zhongtai.system.mapper.TaskMapper;
import cn.cnic.zhongtai.system.model.ColumnSimpleInfo;
import cn.cnic.zhongtai.system.model.DataChangeLog;
import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.model.vo.DataClearVO;
import cn.cnic.zhongtai.system.model.vo.DataOperationVO;
import cn.cnic.zhongtai.system.service.DataClearService;
import cn.cnic.zhongtai.system.service.DataImportService;
import cn.cnic.zhongtai.system.service.HiveService;
import cn.cnic.zhongtai.utils.CommonUtils;
import cn.cnic.zhongtai.utils.DateUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Transactional
@Service
@Slf4j
public class DataClearServiceImpl implements DataClearService {



    //需要转义的特殊符号(否则脚本无法识别)
    String[] symbolArray = new String[]{ ".","?","？","_","*","$","^","\\","+","|"};



    @Value("${syspara.HiveDbName}")
    private String HiveDbName;

    @Autowired
    private HiveDao hiveDao;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private DataChangeLogMapper dataChangeLogMapper;

    @Resource
    private Launcher launcher;

    @Resource
    private HiveService hiveService;

    @Resource
    private ParticipleMapper participleMapper;

    @Resource
    private DataImportService dataImportService;

    public Map<String,Object> runJobToClearData(DataClearVO dataClearVO){
        Map<String, Object> map = new HashMap<>();
        map.put("code", 500);
        JSONObject obj = null;
        List<ColumnSimpleInfo> columnList = null;
        //准备表所有字段信息
        try {
            columnList =  hiveDao.getColumnsByTableName(dataClearVO.getTableName());
            if(null != columnList && !columnList.isEmpty()){
                //拼接参数
                obj = createJSONObject(dataClearVO,columnList);
            }
        }catch (Exception e) {
            log.error(dataClearVO.getTableName()+":property query failed from hive;"+e);
            throw new RuntimeException("property query failed from hive");
        }
        log.info(obj.toString());
        //提交spark任务，返回applicationId
        String logFilePath = "/data/tobaccoZhongtai/spark/log/dataClear-" + DateUtils.dateToStr_YYYYMMDD(new Date())+"-"+ CommonUtils.getCode(8) + ".log";

        File logFile = new File(logFilePath);
        try {
            logFile.createNewFile();
            //创建任务返回任务的id
            int taskId = createTask(dataClearVO.getTableName(), logFilePath,"dataClearTask");

            //提交spark任务
            Map<String, Object> jsonParam = new HashMap<>();
            jsonParam.put("operationMode", "clear");//操作方式
            jsonParam.put("table",  dataClearVO.getTableName());//表名
            jsonParam.put("newTableName",  dataClearVO.getNewTableName());//新表名,如果是空则不新建
            jsonParam.put("dataClearVO", dataClearVO);

            TaskListener taskListener = new TaskListener(String.valueOf(taskId), logFilePath, "",null,JSON.toJSONString(jsonParam));
            launcher.CreateSparkLauncher( Constant.SPARK_JAR_CLEAR, obj.toString(), taskListener, logFile);
            log.info("清洗任务jar开始执行；参数："+obj.toString());
            //记录变更日志
            createCleanLog(dataClearVO,columnList);
            map.put("msg", "数据清洗任务添加完成");
            map.put("taskId", taskId);
            map.put("code", 200);
        } catch (IOException e) {
            log.error("spark清洗任务提交异常,"+e);
            throw new RuntimeException("spark清洗任务提交异常,"+e);
        }
        return map;

    }



    public void createCleanLog(DataClearVO dataClearVO,List<ColumnSimpleInfo> columnList){
        for (DataOperationVO column : dataClearVO.getDataOperation()) {
            DataChangeLog changeLog = new DataChangeLog();
            changeLog.setTableName(dataClearVO.getTableName());

            changeLog.setMode(column.getMode());
            try {
                InetAddress ip4 = Inet4Address.getLocalHost();
                changeLog.setOperationIp(ip4.getHostAddress());
            } catch (UnknownHostException e) {
                changeLog.setOperationIp(null);
                log.error("ip获取异常,"+e);
            }
            changeLog.setOperationTime(new Date());
            changeLog.setParam(createJSONObject(dataClearVO,columnList).toString());
            changeLog.setOperationUser("wdd");
            changeLog.setVersion("0.1");
            dataChangeLogMapper.insert(changeLog);

        }


    }

    public int createTask(String tableName , String logFilePath,String type) {
        Map<String, String> jsonParam = new HashMap<>();
        jsonParam.put("logFilePath", logFilePath);
        Task task = new Task();
        task.setCreateTime(new Date());
        task.setTaskType(type);
        task.setTaskName(tableName+"-"+type);
        task.setStatus(Task.WAITING);
        task.setJsonData(JSON.toJSONString(jsonParam));
        taskMapper.createTask(task);
        return  task.getTaskId();
    }

    // 创建JSONObject对象
    private  JSONObject createJSONObject(DataClearVO dataClearVO,List<ColumnSimpleInfo> columnList) {
        JSONObject result = new JSONObject();
       // result.put("table", HiveDbName+"."+dataClearVO.getTableName());//表名
        result.put("table",  dataClearVO.getTableName());//表名
        result.put("newTableName",  dataClearVO.getNewTableName());//新表名,如果是空则不新建


        //字段准备
        JSONArray column = new JSONArray();
        for(int i=0;i<columnList.size();i++){
            column.add(i, columnList.get(i).getColumnName());
        }

        // 返回一个JSONArray对象
        JSONArray jsonArray = new JSONArray();
        for(int i=0;i<dataClearVO.getDataOperation().size();i++){
            JSONObject T = new JSONObject();
            DataOperationVO dataOperation = dataClearVO.getDataOperation().get(i);
            if(null != dataOperation.getType()){
                T.put("type", dataOperation.getType());
            }
            if(null != dataOperation.getContent()){
                T.put("content", dataOperation.getContent());
            }
            if(null != dataOperation.getLength()){
                T.put("length", dataOperation.getLength());
            }
            if(null != dataOperation.getMode()){
                T.put("mode", dataOperation.getMode());
            }
            if(null != dataOperation.getPrefix()){
                boolean falg = isSymbol(dataOperation.getPrefix());
                if(!falg){
                    T.put("sign", "\\\\\\\\"+dataOperation.getPrefix());
                }else{
                    T.put("prefix",dataOperation.getPrefix());
                }
            }
            if(null != dataOperation.getSign()){
                boolean falg = isSymbol(dataOperation.getSign());
                if(!falg){
                    T.put("sign", "\\\\\\\\"+dataOperation.getSign());
                }else{
                    T.put("sign", dataOperation.getSign());
                }
            }
            if(null != dataOperation.getSuffix()){
                T.put("suffix", dataOperation.getSuffix());
            }
            if(null != dataOperation.getSplit_column()){
                T.put("split_column", dataOperation.getSplit_column());
            }
            if(null != dataOperation.getColumn()){
                T.put("column", dataOperation.getColumn());
            }
            jsonArray.add(i, T);

        }

        result.put("data",jsonArray);
        result.put("columns" , column);

        return result;
    }


    /**
     * 验证是否是符号
     * @param symbol
     * @return
     */
    public boolean isSymbol(String symbol) {
        for (String info : symbolArray) {
            if(info.equals(symbol)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public Map<String,Object> participleeColumn(DataClearVO dataClearVO) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 500);
        JSONObject obj = null;
        List<ColumnSimpleInfo> columnList = new ArrayList<> ();

        List<DataOperationVO> dataOperation = dataClearVO.getDataOperation();
        if(!dataOperation.isEmpty() && "createMiddleTable".equals(dataOperation.get(0).getType())){

            String prefix = dataOperation.get(0).getPrefix();
            dataOperation.get(0).setPrefix(null);
            if (StringUtils.isNotBlank(prefix) && prefix.contains(",")) {
                String prefixArr[] = prefix.split(",");
                for (int i = 0; i < prefixArr.length; i++) {
                    ColumnSimpleInfo columnSimpleInfo = new ColumnSimpleInfo();
                    columnSimpleInfo.setColumnName(prefixArr[i]);
                    columnList.add(columnSimpleInfo);
                }
                obj = createJSONObject(dataClearVO, columnList);
            } else if (StringUtils.isNotBlank(prefix) && !prefix.contains(",")) {
                ColumnSimpleInfo columnSimpleInfo = new ColumnSimpleInfo();
                columnSimpleInfo.setColumnName(prefix);
                columnList.add(columnSimpleInfo);
                obj = createJSONObject(dataClearVO, columnList);
            } else {
                throw new RuntimeException("行转列参数选择错误");
            }
        }else {
            //准备表所有字段信息
            try {
                columnList =  hiveDao.getColumnsByTableName(dataClearVO.getTableName());
                if(null != columnList && !columnList.isEmpty()){
                    //拼接参数
                    obj = createJSONObject(dataClearVO,columnList);
                }
            }catch (Exception e) {
                log.error(dataClearVO.getTableName()+":property query failed from hive;"+e);
                throw new RuntimeException("property query failed from hive");
            }
        }

        log.info(obj.toString());
        //提交spark任务，返回applicationId
        String logFilePath = "/data/tobaccoZhongtai/spark/log/dataParticiplee-" + DateUtils.dateToStr_YYYYMMDD(new Date())+"-"+ CommonUtils.getCode(8) + ".log";

        File logFile = new File(logFilePath);
        try {
            logFile.createNewFile();
            //创建任务返回任务的id
            int taskId = createTask(dataClearVO.getTableName(), logFilePath,"participle");

            //提交spark任务
            //判断分词执行的jar
            Map<String, Object> jsonParam = new HashMap<>();
            jsonParam.put("operationMode", "participle");//操作方式
            jsonParam.put("table",  dataClearVO.getTableName());//表名
            jsonParam.put("newTableName",  dataClearVO.getNewTableName());//新表名,如果是空则不新建
            jsonParam.put("dataMappingId", dataClearVO.getPid());
            jsonParam.put("dataMappingName", dataClearVO.getDataMappingName());
            jsonParam.put("newColumnName", dataOperation.get(0).getContent());// 分词功能关键词新加列的列名
            //字段准备  为了创建模型做准备
            JSONArray column = new JSONArray();
            for(int i=0;i<columnList.size();i++){
                column.add(i, columnList.get(i).getColumnName());
            }
            //这里添加是为了添加模型的时候把分词新加的列也添加上
            column.add(column.size(),dataClearVO.getDataOperation().get(0).getContent());
            jsonParam.put("column", column);

            TaskListener taskListener = new TaskListener(String.valueOf(taskId), logFilePath, "",null,JSON.toJSONString(jsonParam));
            launcher.CreateSparkLauncher( Constant.PARTICIPLE_JAR, obj.toString(), taskListener, logFile);
            log.info("分词jar开始执行；参数："+obj.toString());
            //记录变更日志
            createCleanLog(dataClearVO,columnList);
            map.put("msg", "数据清洗任务添加完成");
            map.put("taskId", taskId);
            map.put("code", 200);
        } catch (IOException e) {
            log.error("spark清洗任务提交异常,"+e);
            throw new RuntimeException("spark清洗任务提交异常,"+e);
        }
        return map;
    }


    @Override
    public void statistical_Information(String tableName,List<ColumnSimpleInfo> columnList){
        if(StringUtils.isBlank(tableName) || null == columnList){
            return  ;
        }
        //判断当前是否有任务正在执行着
        List<Task> list = taskMapper.getTasksBytaskName(tableName+"-statisticalInformation");
        if (!list.isEmpty()){
            for (Task task : list) {
                if (Task.WAITING.equals(task.getStatus()) || Task.SUBMITTED.equals(task.getStatus()) || Task.DOING.equals(task.getStatus())) {
                    return  ;
                }
            }
        }
        int num = 0;
        //检查是否已有统计信息表
        for (ColumnSimpleInfo column : columnList) {
            String groupTableName = "group_" + tableName + "_" + column.getColumnName();
            //判断表是否存在
            boolean rows = hiveService.validateTableNameExistByHive(groupTableName);
            if (rows) {
                num++;
            }
        }
        //如果已有并且数量与字段数量一致,则不执行..
        if (num > 0 && num == columnList.size()) {
            return;
        }

        JSONObject result = new JSONObject();
        result.put("table", tableName);//表名
        //字段准备
        JSONArray column = new JSONArray();
        for(int i=0;i<columnList.size();i++){
            column.add(i, columnList.get(i).getColumnName());
        }
        result.put("columns" , column);

        log.info("统计分析参数："+result.toString());
        //提交spark任务，返回applicationId
        String logFilePath = "/data/tobaccoZhongtai/spark/log/statisticalInformation-" + DateUtils.dateToStr_YYYYMMDD(new Date())+"-"+ CommonUtils.getCode(8) + ".log";

        //记录任务
        int taskId = createTask(tableName,logFilePath,"statisticalInformation");

        File logFile = new File(logFilePath);
        try {
            logFile.createNewFile();
            StatisticalInformationListener listener = new StatisticalInformationListener(String.valueOf(taskId),logFilePath,"");
            launcher.CreateSparkLauncher( Constant.DATA_STATISTICAL_INFORMATION_JAR, result.toString(), listener, logFile);
            log.info("统计分析开始执行；参数："+result.toString());
        } catch (IOException e) {
            log.error("spark清洗任务提交异常,"+e);
        }
    }
    }
