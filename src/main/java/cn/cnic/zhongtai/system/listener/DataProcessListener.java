package cn.cnic.zhongtai.system.listener;

import cn.cnic.zhongtai.system.entity.node.NodeGraph;
import cn.cnic.zhongtai.system.model.DataMapping;
import cn.cnic.zhongtai.system.model.GenTable;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.service.*;
import cn.cnic.zhongtai.utils.BaseHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkAppHandle;

import java.util.*;

@Slf4j
public class DataProcessListener implements SparkAppHandle.Listener {

    private String taskId;
    private Integer dataProcessId;
    private TaskService taskService;
    private Map<String, Object> param;
    private GenTableService genTableService;
    private DataMappingService dataMappingService;
    private GraphService graphService;
    private DataProcessService dataProcessService;

    public DataProcessListener() {
    }

    public DataProcessListener(String taskId, Integer dataProcessId, Map<String, Object> param) {
        this.taskId = taskId;
        this.param = param;
        this.dataProcessId = dataProcessId;
        this.taskService = BaseHolder.getBean("taskServiceImpl");
        this.genTableService = BaseHolder.getBean("genTableServiceImpl");
        this.dataMappingService = BaseHolder.getBean("dataMappingServiceImpl");
        this.graphService = BaseHolder.getBean("graphServiceImpl");
        this.dataProcessService = BaseHolder.getBean("dataProcessServiceImpl");
    }

    @Override
    public void stateChanged(SparkAppHandle handle) {
        if (handle.getState().equals(SparkAppHandle.State.FAILED)) {
            taskService.changeTaskStatus(Integer.valueOf(taskId), "failed");
            dataProcessService.updateStatusById(dataProcessId, "failed");
        } else if (handle.getState().equals(SparkAppHandle.State.FINISHED)) {
            handle();
            taskService.changeTaskStatus(Integer.valueOf(taskId), "done");
            dataProcessService.updateStatusById(dataProcessId, "done");
        } else if (handle.getState().equals(SparkAppHandle.State.SUBMITTED)) {
            if (null != handle.getAppId() && !"null".equals(handle.getAppId())) {
                taskService.changeTaskAppId(Integer.valueOf(taskId), handle.getAppId());
                log.info("================" + handle.getState() + "状态===============成功更新appid " + handle.getAppId() + "===============================");
            }
        } else if (handle.getState().equals(SparkAppHandle.State.RUNNING)) {
            taskService.changeTaskStatus(Integer.valueOf(taskId), "running");
            dataProcessService.updateStatusById(dataProcessId, "running");
        }
    }

    @Override
    public void infoChanged(SparkAppHandle handle) {
    }

    private void handle() {
        //
        //创建模型信息
        List<String> columns = (List<String>) param.get("columns");
        String hiveTableName = (String) param.get("hiveTableName");
        String virtualTableName = (String) param.get("virtualTableName");
        String pid = (String) param.get("pid");
        Set<String> fromTables = (Set<String>) param.get("fromTables");

        List<GenTableColumn> genTableColumns = new ArrayList<>(columns.size());
        for (String columnName : columns) {
            GenTableColumn column = new GenTableColumn();
            column.setColumnName(columnName);
            column.setColumnType("string");
            genTableColumns.add(column);
        }
        GenTable genTable = new GenTable();
        genTable.setTableName(hiveTableName);
        genTable.setGenTableColumn(genTableColumns);
        genTableService.insertGenTable(genTable);

        //虚拟目录添加记录
        DataMapping dataMapping = new DataMapping();
        dataMapping.setCreateTime(new Date());
        dataMapping.setUpdateTime(new Date());
        dataMapping.setName(virtualTableName);
        dataMapping.setTableName(hiveTableName);
        dataMapping.setParentId(Integer.valueOf(pid));
        dataMapping.setStatus("0");
        dataMapping.setModelId(genTable.getTableId().intValue());
        //类型为文件
        dataMapping.setType(1);
        dataMappingService.insert(dataMapping);

        //添加血缘关系 添加两个节点  一个是数据库 一个是新建的table

        NodeGraph target = NodeGraph.builder()
                .table_name(hiveTableName)
                .build();

        graphService.createNode(target);

        for (String nodeName : fromTables) {
            NodeGraph source = NodeGraph.builder()
                    .table_name(nodeName)
                    .build();
            graphService.createNode(source);
            graphService.createRelationByTableName(nodeName, hiveTableName);
        }

    }
}
