package cn.cnic.zhongtai.system.scheduled;

import cn.cnic.zhongtai.system.handler.*;
import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.BaseHolder;
import cn.cnic.zhongtai.utils.IpAddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class TaskScheduled {

    @Resource
    private TaskService taskService;
    private TaskHandler taskHandler;

    //@Scheduled(fixedDelay = 10 * 1000)
    public void runTask() {
        log.error("execute task");
        Task oneTask = taskService.getOneTask();
        if (oneTask == null) {
            return;
        }
        // 任务类型为ImportTask
        if (oneTask.getTaskType().equals("ImportData")) {
            taskHandler = (ImportDataTaskHandler) BaseHolder.getBean("importDataTaskHandler");
            taskHandler.handle(oneTask);
        }

    }

    //@Scheduled(fixedDelay = 5 * 1000)
    public void runTask2() {
        taskHandler = (TopicHandler) BaseHolder.getBean("topicHandler");
        taskHandler.handle(null);
    }


    //@Scheduled(fixedDelay = 5 * 1000)
    public void updateWaitingTaskStatus(){
        if (!IpAddressUtil.getHostIp().equals("127.0.0.1")) {
            return;
        }
        UpdateWaitingTaskHandler taskHandler = BaseHolder.getBean("updateWaitingTaskHandler");
        taskHandler.handle();
    }

    /**
     * 定时去查正在执行任务的任务状态，并更新
     */
    //@Scheduled(fixedDelay = 5 * 1000)
    public void updateDoingTaskStatus(){
        if (!IpAddressUtil.getHostIp().equals("127.0.0.1")) {
            return;
        }
        UpdateDoingTaskHandler taskHandler = BaseHolder.getBean("updateDoingTaskHandler");
        taskHandler.handle();
    }

    /**
     * 导入图库
     */
    //@Scheduled(fixedDelay = 5 * 1000)
    public void mapTaskExecute(){
        if (!IpAddressUtil.getHostIp().equals("127.0.0.1")) {
            return;
        }
        MapTaskHandler taskHandler = BaseHolder.getBean("mapTaskHandler");
        taskHandler.handle();
    }

    @Scheduled(fixedDelay = 5 * 1000)
    public void dataImportJDBC(){
        if (!IpAddressUtil.getHostIp().equals("127.0.0.1")) {
            return;
        }
        DataImportJDBCHandler taskHandler = BaseHolder.getBean("dataImportJDBCHandler");
        taskHandler.handle();
    }

    @Scheduled(fixedDelay = 5 * 1000)
    public void neo4jToHive(){
        if (!IpAddressUtil.getHostIp().equals("127.0.0.1")) {
            return;
        }
        Neo4jToHiveHandler neo4jToHiveHandler = BaseHolder.getBean("neo4jToHiveHandler");
        neo4jToHiveHandler.handler();
    }

    /**
     * 数据清洗任务
     */
    /*@Scheduled(fixedDelay = 5 * 1000)
    public void runTaskDataClear() {
        log.error("execute dataClearHandler task");
        DataClearHandler dataClearHandler =  BaseHolder.getBean("dataClearHandler");
        dataClearHandler.handle(null);

    }*/


}
