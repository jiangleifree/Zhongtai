package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.system.mapper.TaskMapper;
import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.SparkUtils;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class TaskServiceImpl implements TaskService {
    @Resource
    private TaskMapper taskMapper;

    @Override
    public Task getOneTask() {
        return taskMapper.getOneTask();
    }

    @Override
    public int createTask(Task task) {
        return taskMapper.createTask(task);
    }

    @Override
    public void changeTaskStatus(int taskId, String status) {
        taskMapper.changeTaskStatus(taskId,status);
    }

    @Override
    public void changeTaskAppId(int taskId, String appId){
        taskMapper.changeTaskAppId(taskId,appId);
    }

    @Override
    public List<Task> getAllTask() {
        return taskMapper.getAllTask();
    }

    @Override
    public void updateNextTime(int taskId, Date nextTime) {
        taskMapper.updateNextTime(taskId, nextTime);
    }

    @Override
    public boolean hasDoingTaskByTaskType(String taskType) {
        int count = taskMapper.getCountByTaskType(taskType);
        return count != 0;
    }

    @Override
    public Task getOneDoingTaskByTaskType(String taskType) {
        return taskMapper.getOneDoingTaskByTaskType(taskType);
    }

    @Override
    public Task getOneWaitingTaskByTaskType(String taskType) {
        return taskMapper.getOneWaitingTaskByTaskType(taskType);
    }

    @Override
    public void updateJsonData(int taskId, String jsonData) {
        taskMapper.updateJsonData(taskId, jsonData);
    }

    @Override
    public List<Task> getTasksByStatus(String status) {
        return taskMapper.getTasksByStatus(status);
    }

    @Override
    public Task getOneMapTask() {
        return taskMapper.getOneMapTask();
    }

    @Override
    public Task getTaskStatusById(int taskId) {
        return taskMapper.getTaskStatusById(taskId);
    }

    @Override
    public List<Task> getTasksByTypeAndStatus(String taskType, String status) {
        return taskMapper.getTasksByTypeAndStatus(taskType, status);
    }

    @Override
    public void changeTaskToFailed(int taskId, String cause) {
        taskMapper.changeTaskStatusAndErrCause(taskId, Task.FAILED, cause);
    }

    @Override
    public void addLog(int taskId, String log) {
        taskMapper.addLog(taskId, log);
    }

    @Override
    public String getSparkLogByAppId(String applicationId){

        //ThirdFlowLog thirdFlowLog = null;
        String amContainerLogs = "";
        String str = SparkUtils.getSparkLogUrl(applicationId, Constant.SPARK_LOG_URL);
        if (StringUtils.isNotBlank(str) && !str.contains("Exception")) {
            log.info("Successful call : " + str);
            // Also convert the json string to a json object, and then convert the json object to a java object, as shown below.
            JSONObject obj = JSONObject.parseObject(str);// Convert a json string to a json object
            if (null != obj) {
                JSONObject app = obj.getJSONObject("app");
                if (null != app) {
                    amContainerLogs = app.getString("amContainerLogs");
                }
            }
            // Convert a json object to a java object
            // thirdFlowLog = (ThirdFlowLog) JSONObject.toBean(obj, ThirdFlowLog.class);
        } else {
            log.info("call failed : " + str);
        }
        // return thirdFlowLog;
        return amContainerLogs;

    }
}
