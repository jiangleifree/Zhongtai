package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.TableInfo;

import java.util.List;

public interface MysqlSchemaMapper {

    List<TableInfo> getAllTablesInfo(String dbName);

    TableInfo getTableInfoByDBNameAndTableName(String dbName, String tableName);

    List<String> getAllFieldsByDBNameAndTableName(String dbName, String tableName);

    List<TableInfo> getAllFieldsByDBName(String dbName);
}
