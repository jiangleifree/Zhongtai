package cn.cnic.zhongtai.system.listener;

import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.DataImportService;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.BaseHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.launcher.SparkAppHandle;

import java.io.*;
import java.util.Map;

@Slf4j
public class TaskListener implements SparkAppHandle.Listener {
    private TaskService taskService;
    private DataImportService dataImportService;
    private String taskId;
    private String stdputLogPath;
    private String errputLogPath;
    private Long dataImportId; //数据接入id
    private String jsonParam;

    public TaskListener() {
    }

    public TaskListener(String taskId, String stdputLogPath, String errputLogPath, Long dataImportId, String  jsonParam) {
        this.taskId = taskId;
        this.stdputLogPath = stdputLogPath;
        this.errputLogPath = errputLogPath;
        this.dataImportId = dataImportId;
        this.jsonParam = jsonParam;
        taskService = BaseHolder.getBean("taskServiceImpl");
        dataImportService = BaseHolder.getBean("dataImportServiceImpl");
    }

    @Override
    public void stateChanged(SparkAppHandle handle) {
        if (handle.getState().equals(SparkAppHandle.State.FAILED)) {
            taskService.changeTaskStatus(Integer.valueOf(taskId), Task.FAILED);
            setStatus(Task.FAILED);
            //TODO 这里要取日志
            addLog();
        } else if (handle.getState().equals(SparkAppHandle.State.FINISHED)) {
            taskService.changeTaskStatus(Integer.valueOf(taskId),Task.DONE);
            if(StringUtils.isNotBlank(jsonParam) ){
                dataImportService.createModelAndMapping(jsonParam);
            }
            setStatus(Task.DONE);
            //TODO  这里要取日志
            addLog();
        } else if (handle.getState().equals(SparkAppHandle.State.SUBMITTED)){
            taskService.changeTaskStatus(Integer.valueOf(taskId), Task.SUBMITTED);
            setStatus(Task.SUBMITTED);
            if(null != handle.getAppId() && !"null".equals(handle.getAppId())){
                taskService.changeTaskAppId(Integer.parseInt(taskId), handle.getAppId());
                log.info("================"+handle.getState()+"状态===============成功更新appid "+handle.getAppId()+"===============================");
            }
        }else if (handle.getState().equals(SparkAppHandle.State.RUNNING)){
            taskService.changeTaskStatus(Integer.valueOf(taskId), Task.DOING);
            setStatus(Task.DOING);
        }

    }

    /**
     * 数据接入状态修改
     * @param status
     */
    private void setStatus(String status){
        if(null != dataImportId){
            dataImportService.changeDataImportProgress(dataImportId,status);
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
