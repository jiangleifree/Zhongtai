package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.TableInfo;

import java.util.List;

public interface MysqlSchemaService {
    List<TableInfo> getAllTablesInfo(String dbName);

    List<String> getAllFieldsByDBNameAndTableName(String dbName, String tableName);

    TableInfo getTableInfoByDBNameAndTableName(String dbName, String tableName);

    List<TableInfo> getAllFieldsByDBName(String dbName);
}
