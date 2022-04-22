package cn.cnic.zhongtai.system.handler;

import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.DataImportService;
import cn.cnic.zhongtai.system.service.TaskService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.List;

import static cn.cnic.zhongtai.system.Constant.INCLUDE_TASK;

@Component
@Slf4j
public class UpdateWaitingTaskHandler {

    @Resource
    private TaskService taskService;

    @Resource
    private DataImportService dataImportService;

    public void handle() {

        //获取所有waitingTask
        List<Task> waitingTasks = taskService.getTasksByStatus(Task.WAITING);
        for (int i = 0; i < waitingTasks.size(); i++) {
            Task temp = waitingTasks.get(i);
            if (!INCLUDE_TASK.contains(temp.getTaskType())){ //判断任务类型是否在可处理范围内
                return;
            }
            JSONObject param = JSON.parseObject(temp.getJsonData());
            String logFilePath = param.getString("logFilePath");
            String appId = param.getString("applicationId");
            //查日志
            String status = searchStatusByLogFile(logFilePath, appId);
            if (status.equals("doing")){
                //数据接入任务需要更新数据接入表状态
                //if("dataToHiveByHdfsTask".equals(temp.getTaskType())){
                if (Task.dataToHiveByHdfsTask.equals(temp.getTaskType())){
                    String id = param.getString("id");
                    dataImportService.changeDataImportProgress(Long.valueOf(id),Task.DOING);
                }
                taskService.changeTaskStatus(temp.getTaskId(), Task.DOING);
            }
        }
    }

    private String searchStatusByLogFile(String logFilePath, String appId) {

        String flag = "INFO YarnClientSchedulerBackend: Application "+appId+" has started running";
        File logFile = new File(logFilePath);
        if (logFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    int appIdFlag = line.indexOf(flag);
                    if (appIdFlag != -1) {
                        return "doing";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return "waiting";
    }
}
