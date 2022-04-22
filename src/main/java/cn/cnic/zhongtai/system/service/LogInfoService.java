package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.LogInfo;
import cn.cnic.zhongtai.system.model.Task;

import java.util.List;

public interface LogInfoService {

    List<LogInfo> getLogInfoByTaskId(int taskId);
    void insertLogInfo(Task task, String logInfo);
 }
