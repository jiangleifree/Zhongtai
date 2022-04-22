package cn.cnic.zhongtai.system.listener;

import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.BaseHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkAppHandle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

@Slf4j
public class StatisticalInformationListener implements SparkAppHandle.Listener {
    private TaskService taskService;
    private String stdputLogPath;
    private String errputLogPath;
    private String taskId;

    public StatisticalInformationListener(String taskId,String stdputLogPath, String errputLogPath) {
        this.taskId = taskId;
        this.stdputLogPath = stdputLogPath;
        this.errputLogPath = errputLogPath;
        taskService = BaseHolder.getBean("taskServiceImpl");
    }



    @Override
    public void stateChanged(SparkAppHandle handle) {
        if (handle.getState().equals(SparkAppHandle.State.FAILED)) {
            log.info("================数据统计分析执行中===============状态： " + handle.getState() + "================failed================");
            taskService.changeTaskStatus(Integer.valueOf(taskId), Task.FAILED);
            addLog();
        } else if (handle.getState().equals(SparkAppHandle.State.FINISHED)) {
            log.info("================数据统计分析执行中===============状态： " + handle.getState() + "================success===============");
            taskService.changeTaskStatus(Integer.valueOf(taskId),Task.DONE);
            addLog();
        } else if (handle.getState().equals(SparkAppHandle.State.SUBMITTED)) {
            taskService.changeTaskStatus(Integer.valueOf(taskId), Task.SUBMITTED);
            if(null != handle.getAppId() && !"null".equals(handle.getAppId())){
                taskService.changeTaskAppId(Integer.parseInt(taskId), handle.getAppId());
                log.info("=============数据统计分析执行中===：" + handle.getState() + "==========成功更新appid " + handle.getAppId() + "=================");
            }
        } else if (handle.getState().equals(SparkAppHandle.State.RUNNING)) {
            log.info("================数据统计分析执行中===============状态： " + handle.getState() + "=================doing================");
            taskService.changeTaskStatus(Integer.valueOf(taskId), Task.DOING);
        }

    }

    private void addLog(){
        File file = new File(stdputLogPath);
        String line;
        StringBuilder sb = new StringBuilder();
        if (file.exists()){
            try {
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader reader = new InputStreamReader(fis);
                BufferedReader bf = new BufferedReader(reader);
                while(null != (line = bf.readLine())) {
                    sb.append(line).append("\n"); //注意readline时会把换行符弄没了
                }
                //塞日志
                taskService.addLog(Integer.valueOf(taskId), sb.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void infoChanged(SparkAppHandle handle) {

    }
}
