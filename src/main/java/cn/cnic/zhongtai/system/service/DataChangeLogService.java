package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.DataChangeLog;

import java.util.List;

public interface DataChangeLogService {

    List<DataChangeLog> getLogInfoByTableId(int tableId);

    void insertDataChangeLog(DataChangeLog dataChangeLog);

    void insertDataChangeLogSelective(DataChangeLog dataChangeLog);

    List<DataChangeLog> getDataChangeLogByTableName(String tableName);
 }
