package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.MysqlSchemaMapper;
import cn.cnic.zhongtai.system.model.TableInfo;
import cn.cnic.zhongtai.system.service.MysqlSchemaService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class MysqlSchemaServiceImpl implements MysqlSchemaService {

    @Resource
    private MysqlSchemaMapper mysqlSchemaMapper;

    @Override
    public List<TableInfo> getAllTablesInfo(String dbName) {
        return mysqlSchemaMapper.getAllTablesInfo(dbName);
    }

    @Override
    public List<String> getAllFieldsByDBNameAndTableName(String dbName, String tableName) {
        return mysqlSchemaMapper.getAllFieldsByDBNameAndTableName(dbName, tableName);
    }

    @Override
    public TableInfo getTableInfoByDBNameAndTableName(String dbName, String tableName) {
        return mysqlSchemaMapper.getTableInfoByDBNameAndTableName(dbName, tableName);
    }

    @Override
    public List<TableInfo> getAllFieldsByDBName(String dbName) {
        return mysqlSchemaMapper.getAllFieldsByDBName(dbName);
    }
}
