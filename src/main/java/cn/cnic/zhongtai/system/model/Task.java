package cn.cnic.zhongtai.system.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
public class Task {

    public static final String FAILED = "failed";
    public static final String DOING = "doing";
    public static final String DONE = "done";
    public static final String WAITING = "waiting";
    public static final String SUBMITTED = "submitted";
    public static final String dataToHiveByHdfsTask = "dataToHiveByHdfsTask";
    public static final String TopicTask = "TopicTask";
    public static final String Neo4jTask = "Neo4jTask";
    public static final String ONLINE_IMPORT_DATA_BY_JDBC = "OnlineImportDataByJDBCTask";
    public static final String NEO4J_TO_HIVE_TASK = "Neo4jToHiveTask";
    public static final String DATA_CLEAR_TASK = "dataClearTask";

    //任务id
    private int taskId;

    //任务名字
    private String taskName;
    //任务类型
    private String taskType;
    //创建时间
    @JsonFormat(locale="zh", timezone="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    //下次执行时间
    @JsonFormat(locale="zh", timezone="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date nextTime;
    //时间间隔
    private long interval;
    //任务状态
    private String status;
    //data
    private String jsonData;
    //日志
    private List<LogInfo> logInfos;
    //任务失败可能原因
    private String errorCause;

    //appId
    private String appId;


}
