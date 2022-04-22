package cn.cnic.zhongtai.system.listener;

import cn.cnic.zhongtai.system.service.DataFusionService;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.BaseHolder;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkAppHandle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

@Slf4j
public class DataFusionListener implements SparkAppHandle.Listener {
    private TaskService taskService;
    private String taskId;
    private String stdputLogPath;
    private String errputLogPath;
    private DataFusionService dataFusionService;
    private String tableName;
    private JSONObject data;

    public DataFusionListener() {
    }

    public DataFusionListener(String taskId, String stdputLogPath,
                              String errputLogPath, String tableName,
                              JSONObject data) {
        this.taskId = taskId;
        this.stdputLogPath = stdputLogPath;
        this.errputLogPath = errputLogPath;
        this.tableName = tableName;
        this.data = data;
        taskService = BaseHolder.getBean("taskServiceImpl");
        dataFusionService = BaseHolder.getBean("dataFusionServiceImpl");
    }

    @Override
    public void stateChanged(SparkAppHandle handle) {
        if (handle.getState().equals(SparkAppHandle.State.FAILED)) {
            taskService.changeTaskStatus(Integer.valueOf(taskId), "融合任务失败");
            addLog();
        } else if (handle.getState().equals(SparkAppHandle.State.FINISHED)) {
            taskService.changeTaskStatus(Integer.valueOf(taskId),"融合任务完成");
            //融合任务完成后创建虚拟目录以及相关信息
            dataFusionService.doSuccessTask(tableName, data);
            addLog();
        } else if (handle.getState().equals(SparkAppHandle.State.SUBMITTED)){
            taskService.changeTaskStatus(Integer.valueOf(taskId), "准备执行融合任务");
            if(null != handle.getAppId() && !"null".equals(handle.getAppId())){
                taskService.changeTaskAppId(Integer.parseInt(taskId), handle.getAppId());
                log.info("================"+handle.getState()+"状态===============成功更新appid "+handle.getAppId()+"===============================");
            }
        }else if (handle.getState().equals(SparkAppHandle.State.RUNNING)){
            taskService.changeTaskStatus(Integer.valueOf(taskId), "正在执行融合任务");
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
