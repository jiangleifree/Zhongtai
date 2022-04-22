package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.Task;

import java.util.Date;
import java.util.List;

public interface TaskService {

    Task getOneTask();
    int createTask(Task task);
    void changeTaskStatus(int taskId, String status);

    void changeTaskAppId(int taskId, String appId);
    List<Task> getAllTask();

    void updateNextTime(int taskId, Date nextTime);

    boolean hasDoingTaskByTaskType(String taskType);

    Task getOneDoingTaskByTaskType(String taskType);

    Task getOneWaitingTaskByTaskType(String taskType);

    void updateJsonData(int taskId, String jsonData);

    List<Task> getTasksByStatus(String status);

    Task getOneMapTask();

    Task getTaskStatusById(int taskId);

    List<Task> getTasksByTypeAndStatus(String taskType, String status);

    void changeTaskToFailed(int taskId, String cause);

    void addLog(int taskId, String log);

    /**
     * getSparkLogByAppId
     *
     * @param applicationId
     * @return
     */
     String getSparkLogByAppId(String applicationId);
}
