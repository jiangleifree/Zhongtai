package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.Task;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface TaskMapper {
    int createTask(Task task);
    void changeTaskStatus(@Param("taskId") int taskId, @Param("status")String status);

    void changeTaskAppId(@Param("taskId") int taskId, @Param("appId")String appId);
    Task getOneTask();
    List<Task> getAllTask();

    void updateNextTime(int taskId, Date nextTime);

    int getCountByTaskType(String taskType);

    Task getOneDoingTaskByTaskType(String taskType);

    Task getOneWaitingTaskByTaskType(String taskType);

    void updateJsonData(int taskId, String jsonData);

    List<Task> getTasksByStatus(String status);

    Task getOneMapTask();

    Task getTaskStatusById(int taskId);

    List<Task> getTasksByTypeAndStatus(@Param("taskType") String taskType, @Param("status") String status);

    void changeTaskStatusAndErrCause(int taskId, String status, String errorCause);

    void addLog(int taskId, String log);

    List<Task> getTasksBytaskName(@Param("taskName") String taskName);
}
