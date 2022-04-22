package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.DataChangeLogMapper;
import cn.cnic.zhongtai.system.model.DataChangeLog;
import cn.cnic.zhongtai.system.service.DataChangeLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
public class DataChangeLogServiceImpl implements DataChangeLogService {

    @Resource
    private DataChangeLogMapper dataChangeLogMapper;


    @Override
    public List<DataChangeLog> getLogInfoByTableId(int tableId) {
       return null;
    }

    @Override
    public void insertDataChangeLog(DataChangeLog dataChangeLog) {
        dataChangeLogMapper.insert( dataChangeLog);
    }

    @Override
    public void insertDataChangeLogSelective(DataChangeLog dataChangeLog) {
        dataChangeLogMapper.insertSelective(dataChangeLog);
    }


    @Override
    public List<DataChangeLog> getDataChangeLogByTableName(String tableName) {
        return dataChangeLogMapper.getDataChangeLogByTableName(tableName);
    }
}
