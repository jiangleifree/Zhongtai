package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.LogInfoMapper;
import cn.cnic.zhongtai.system.model.LogInfo;
import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.LogInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class LogInfoServiceImpl implements LogInfoService {

    @Resource
    private LogInfoMapper logInfoMapper;
    @Override
    public List<LogInfo> getLogInfoByTaskId(int taskId) {
        return logInfoMapper.getLogInfoByTaskId(taskId);
    }

    @Override
    public void insertLogInfo(Task task, String logInfo) {
        logInfoMapper.insertLogInfo(new LogInfo().setCreateTime(new Date()).setTaskId(task.getTaskId()).setLogInfo(logInfo));
    }
}
