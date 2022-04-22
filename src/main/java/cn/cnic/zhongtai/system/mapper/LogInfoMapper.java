package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.LogInfo;

import java.util.List;

public interface LogInfoMapper {

    List<LogInfo> getLogInfoByTaskId(int taskId);
    void insertLogInfo(LogInfo logInfo);
 }
