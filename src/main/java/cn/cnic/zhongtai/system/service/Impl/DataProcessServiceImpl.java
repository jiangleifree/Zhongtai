package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.system.config.Launcher;
import cn.cnic.zhongtai.system.listener.DataProcessListener;
import cn.cnic.zhongtai.system.mapper.DataProcessMapper;
import cn.cnic.zhongtai.system.model.DataProcess;
import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.model.vo.GenTableColumnVO;
import cn.cnic.zhongtai.system.model.vo.GenTableVO;
import cn.cnic.zhongtai.system.service.DataProcessService;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.CommonUtils;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.cnic.zhongtai.system.Constant.RELATION_EXTRACT;

@Service
public class DataProcessServiceImpl implements DataProcessService {

    @Resource
    private Launcher launcher;
    @Resource
    private TaskService taskService;
    @Resource
    private DataProcessMapper dataProcessMapper;

    @Override
    public void relationExtractionV1(GenTableVO genTable) {

        //定义hiveTableName
        String hiveTableName = genTable.getTableName() + Calendar.getInstance().getTimeInMillis();

        List<GenTableColumnVO> columns = genTable.getGenTableColumn();

        if (CollectionUtils.isEmpty(columns)) {
            throw new RuntimeException("column参数有误");
        }

        List<String> colNames = columns.stream()
                .map(tabCol -> tabCol.getColumnName())
                .collect(Collectors.toList());
        Set<String> fromTables = columns.stream()
                .map(tabCol -> tabCol.getFromTable().split("\\.")[0])
                .collect(Collectors.toSet());

        //拼接spark参数
        List<Map<String, String>> columnsList = new ArrayList<>();
        for (int j = 0; j < columns.size(); j++) {

            Map<String, String> column = new HashMap<>();
            column.put("columnName", columns.get(j).getColumnName());
//            StringBuilder fromBuilder = new StringBuilder();
//            fromBuilder.append(columns.get(j).getFromTable().split("\\.")[0] + ".");
//            fromBuilder.append("`" + columns.get(j).getFromTable().split("\\.")[1] + "`");
            column.put("from", columns.get(j).getFromTable().replace("`", ""));
            columnsList.add(column);
        }
        String where = genTable.getWhere();
        String[] ors = where.split("or");
        List<String> condition = Arrays.stream(ors).map(or -> or.replace("`","").trim()).collect(Collectors.toList());
        //spark参数
        Map<String, Object> sparkParam = new HashMap<>();
        sparkParam.put("newTableName", hiveTableName);
        sparkParam.put("fromTable", org.apache.commons.lang3.StringUtils.join(fromTables, ","));
        sparkParam.put("columns", columnsList);
        sparkParam.put("condition", condition);

        //回调需要的参数
        Map<String, Object> listenerParam = new HashMap<>();
        listenerParam.put("hiveTableName", hiveTableName);
        listenerParam.put("columns", colNames);
        listenerParam.put("fromTables", fromTables);
        listenerParam.put("pid", genTable.getPid());
        listenerParam.put("virtualTableName", genTable.getTableName());

        Task task = new Task();
        task.setCreateTime(new Date());
        task.setStatus(Task.WAITING);
        task.setTaskType("DataProcessV1");
        task.setTaskName("DataProcessV1");
        taskService.createTask(task);

        DataProcess dataProcess = new DataProcess();
        dataProcess.setCreateDate(new Date());
        dataProcess.setName(StringUtils.join(fromTables, ",") + "->" + genTable.getTableName());
        dataProcess.setType("RelationExtract");
        dataProcess.setStatus("waiting");
        dataProcessMapper.insert(dataProcess);

        DataProcessListener dataProcessListener = new DataProcessListener(String.valueOf(task.getTaskId()), dataProcess.getId(), listenerParam);
        String stdputLogPath = Constant.SPARK_DATA_PROCESS_TASK_LOG_PATH + CommonUtils.getCalenderTimeInMillis();
        //重定向日志
        File logFile = new File(stdputLogPath);
        try {
            logFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //提交spark任务
        try {
            launcher.CreateSparkLauncher(RELATION_EXTRACT, JSON.toJSONString(sparkParam), dataProcessListener, logFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<DataProcess> getList() {
        return dataProcessMapper.getList();
    }

    @Override
    public void updateStatusById(Integer id, String status) {
        dataProcessMapper.updateStatusById(id, status);
    }

    @Override
    public void insert(DataProcess dataProcess) {
        dataProcessMapper.insert(dataProcess);
    }
}
