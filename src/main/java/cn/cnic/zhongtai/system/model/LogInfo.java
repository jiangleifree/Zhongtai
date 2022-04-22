package cn.cnic.zhongtai.system.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

public class LogInfo {
    //id
    private int logId;
    //日志内容
    private String logInfo;
    //所属的任务id
    private int taskId;
    //日志创建时间
    @JsonFormat(locale="zh", timezone="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public int getLogId() {
        return logId;
    }

    public LogInfo setLogId(int logId) {
        this.logId = logId;
        return this;
    }

    public String getLogInfo() {
        return logInfo;
    }

    public LogInfo setLogInfo(String logInfo) {
        this.logInfo = logInfo;
        return this;
    }

    public int getTaskId() {
        return taskId;
    }

    public LogInfo setTaskId(int taskId) {
        this.taskId = taskId;
        return this;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public LogInfo setCreateTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }


}

