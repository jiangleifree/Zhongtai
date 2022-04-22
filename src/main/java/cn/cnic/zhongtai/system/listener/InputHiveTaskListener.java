package cn.cnic.zhongtai.system.listener;

import cn.cnic.zhongtai.system.model.vo.DataImportJDBCVo;
import cn.cnic.zhongtai.system.service.DataImportService;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.BaseHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkAppHandle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;

@Slf4j
public class InputHiveTaskListener implements SparkAppHandle.Listener {
    private TaskService taskService;
    private DataImportService dataImportService;
    private String taskId;
    private String stdputLogPath;
    private String errputLogPath;
    private Long dataImportId; //数据接入id
    private List<DataImportJDBCVo.Table> tables;
    private String url;
    private String pid;

    public InputHiveTaskListener() {
    }

    public InputHiveTaskListener(String taskId, String stdputLogPath,
                                 String errputLogPath, Long dataImportId,
                                 List<DataImportJDBCVo.Table> tables, String url, String pid) {
        this.taskId = taskId;
        this.stdputLogPath = stdputLogPath;
        this.errputLogPath = errputLogPath;
        this.dataImportId = dataImportId;
        this.tables = tables;
        this.pid = pid;
        this.url = url;
        taskService = BaseHolder.getBean("taskServiceImpl");
        dataImportService = BaseHolder.getBean("dataImportServiceImpl");
    }

    @Override
    public void stateChanged(SparkAppHandle handle) {
        if (handle.getState().equals(SparkAppHandle.State.FAILED)) {
            taskService.changeTaskStatus(Integer.valueOf(taskId), "入库任务失败");
            setStatus("入库任务失败");
            //TODO 这里要取日志
            addLog();
        } else if (handle.getState().equals(SparkAppHandle.State.FINISHED)) {
            taskService.changeTaskStatus(Integer.valueOf(taskId),"入库任务完成");
            setStatus("入库任务完成");
            //创建虚拟目录 创建model 创建血缘关系
            dataImportService.doFinalWork(tables, pid, url);
            //TODO  这里要取日志
            addLog();
        } else if (handle.getState().equals(SparkAppHandle.State.SUBMITTED)){
            taskService.changeTaskStatus(Integer.valueOf(taskId), "准备执行入库任务");
            setStatus("准备执行入库任务");
            if(null != handle.getAppId() && !"null".equals(handle.getAppId())){
                taskService.changeTaskAppId(Integer.parseInt(taskId), handle.getAppId());
                log.info("================"+handle.getState()+"状态===============成功更新appid "+handle.getAppId()+"===============================");
            }
        }else if (handle.getState().equals(SparkAppHandle.State.RUNNING)){
            taskService.changeTaskStatus(Integer.valueOf(taskId), "正在执行入库任务");
            setStatus("正在执行入库任务");
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
