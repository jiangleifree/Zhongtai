package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.config.Launcher;
import cn.cnic.zhongtai.system.entity.node.NodeGraph;
import cn.cnic.zhongtai.system.listener.DataFusionListener;
import cn.cnic.zhongtai.system.mapper.DataManageMapper;
import cn.cnic.zhongtai.system.mapper.TaskMapper;
import cn.cnic.zhongtai.system.model.DataMapping;
import cn.cnic.zhongtai.system.model.GenTable;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.DataFusionService;
import cn.cnic.zhongtai.system.service.DataMappingService;
import cn.cnic.zhongtai.system.service.GenTableService;
import cn.cnic.zhongtai.system.service.GraphService;
import cn.cnic.zhongtai.utils.CommonUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static cn.cnic.zhongtai.system.Constant.SPARK_IMPORT_DATA_TASK_LOG_PATH;
import static cn.cnic.zhongtai.system.Constant.SPARK_JAR_FUSION;

@Service
@Transactional
public class DataFusionServiceImpl implements DataFusionService {

    @Resource
    private TaskMapper taskMapper;
    @Resource
    private Launcher launcher;
    @Resource
    private DataMappingService dataMappingService;
    @Resource
    private GraphService graphService;
    @Resource
    private GenTableService genTableService;

    @Override
    public void startFusion(JSONObject data) {

        //对json数据先进行简单处理
        data = parseJson(data);
        //将tableName修改为hiveTableName
        String tableName = data.getJSONObject("fusionTableInfo").getString("tableName");
        String hiveTableName = tableName + Calendar.getInstance().getTimeInMillis();
        data.getJSONObject("fusionTableInfo").put("tableName", hiveTableName);

        //提交spark任务
        /**
         * jdbcUrl userName password table
         */
        Task task = new Task();
        task.setCreateTime(new Date());
        task.setTaskType("DataFusion");
        task.setTaskName("DataFusion");
        task.setStatus(Task.WAITING);
        taskMapper.createTask(task);

        String stdoutLogPath = SPARK_IMPORT_DATA_TASK_LOG_PATH + CommonUtils.getUUID32() + ".log";
        File stdoutLogFile = new File(stdoutLogPath);
        try {
            stdoutLogFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataFusionListener taskListener = new DataFusionListener(
                String.valueOf(task.getTaskId()),stdoutLogPath, "",
                tableName, data);
        String args = JSON.toJSONString(data);

        //提交spark任务
        try {
            launcher.CreateSparkLauncher(SPARK_JAR_FUSION, args, taskListener, stdoutLogFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void doSuccessTask(String tableName, JSONObject data){
        String hiveTableName = data.getJSONObject("fusionTableInfo").getString("tableName");
        String pid = data.getJSONObject("fusionTableInfo").getString("localStorage");

        //创建model并与虚拟文件进行关联
        JSONArray columns = data.getJSONObject("fusionTableInfo").getJSONArray("columns");
        List<GenTableColumn> genTableColumns = new ArrayList<>(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            GenTableColumn genTableColumn = new GenTableColumn();
            genTableColumn.setColumnName(columns.getJSONObject(i).getString("name"));
            genTableColumn.setColumnType(columns.getJSONObject(i).getString("type"));
            genTableColumn.setColumnComment(columns.getJSONObject(i).getString("remark"));
            genTableColumn.setColumnLength(columns.getJSONObject(i).getString("length"));
            genTableColumns.add(genTableColumn);
        }
        GenTable genTable = new GenTable();
        genTable.setTableName(hiveTableName);
        genTable.setGenTableColumn(genTableColumns);
        genTableService.insertGenTable(genTable);

        //插入虚拟文件
        DataMapping dataMapping = new DataMapping();
        dataMapping.setName(tableName);
        dataMapping.setTableName(hiveTableName);
        dataMapping.setCreateTime(new Date());
        dataMapping.setUpdateTime(new Date());
        dataMapping.setParentId(Integer.valueOf(pid));
        dataMapping.setStatus("0");
        dataMapping.setModelId(genTable.getTableId().intValue());
        //类型默认为文件
        dataMapping.setType(1);
        dataMappingService.insert(dataMapping);

        //血缘关系添加
        String[] parentNodes = data.getString("tables").split(",");
        //添加end节点
        graphService.createNode(NodeGraph.builder().table_name(hiveTableName).build());
        for (String node : parentNodes) {
            //创建血缘关系
            graphService.createRelationByTableName(node, hiveTableName);
        }
    }

    private JSONObject parseJson(JSONObject data) {
        JSONArray tables = data.getJSONArray("tables");
        List<String> tableList = new ArrayList<>();
        for (int i = 0; i < tables.size(); i++) {
            tableList.add(tables.getJSONObject(i).getString("realTableName"));
        }
        data.put("tables", StringUtils.join(tableList, ","));

        List<String> sourceFieldsList = new ArrayList<>();
        JSONArray columns = data.getJSONObject("fusionTableInfo").getJSONArray("columns");
        for (int i = 0 ; i < columns.size(); i++) {
            JSONArray sourceField = columns.getJSONObject(i).getJSONArray("sourceField");
            for (int j = 0; j < sourceField.size(); j++) {
                sourceFieldsList.add(sourceField.getString(j));
            }
        }

        Map<String, List<String>> tableInfos = new HashMap<>();
        for (int i = 0; i < sourceFieldsList.size(); i++) {
            String[] split = sourceFieldsList.get(i).split(":");
            String hiveTableNameTemp = split[0];
            String column = split[1];
            if(tableInfos.keySet().contains(hiveTableNameTemp)) {
                tableInfos.get(hiveTableNameTemp).add(column);
            } else {
                List<String> columnsTemp = new ArrayList<>();
                columnsTemp.add(column);
                tableInfos.put(hiveTableNameTemp, columnsTemp);
            }
        }
        data.getJSONObject("fusionTableInfo").putAll(tableInfos);
        return data;
    }
}
