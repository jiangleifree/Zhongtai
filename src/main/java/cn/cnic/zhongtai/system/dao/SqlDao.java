package cn.cnic.zhongtai.system.dao;

import cn.cnic.zhongtai.system.config.jdbcConfig.MysqlMainJdbcBaseDaoImpl;
import cn.cnic.zhongtai.system.model.ColumnSimpleInfo;
import cn.cnic.zhongtai.system.model.vo.GenTableVO;
import cn.cnic.zhongtai.system.model.TableInfo;
import cn.cnic.zhongtai.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class SqlDao extends MysqlMainJdbcBaseDaoImpl {

    public boolean createDatabase(String databaseName) {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("create database if not exists `")
                    .append(databaseName)
                    .append("` character set utf8mb4 collate utf8mb4_general_ci ;");
            this.getJdbcTemplate().execute(sql.toString());
            return true;
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return false;
        }
    }

    public List<String> showTables(String database) {
        try {
            String sql = "show tables in " + database;
            List<String> tables = new ArrayList<>();
            this.getJdbcTemplate().query(sql, new RowMapper<Object>() {
                @Override
                public Object mapRow(ResultSet resultSet, int i) throws SQLException {
                    tables.add(resultSet.getString(1));
                    return null;
                }
            });
            return tables;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 查询db下所有表信息
     *
     * @param dbName
     * @return
     */
    public List<TableInfo> findTablesInfoByDBName(String dbName) {

        List<TableInfo> tableInfos = new ArrayList<>();
        List<String> tables = this.showTables(dbName);
        TableInfo temp = null;
        for (String table : tables) {
            List<ColumnSimpleInfo> columns = this.getColumnsByTableName(dbName, table);
            temp = new TableInfo();
            temp.setTopicName(dbName);
            temp.setColumnSampleInfos(columns);
            temp.setTableName(table);
            tableInfos.add(temp);
        }
        return tableInfos;
    }

    /**
     * 根据tableName查询table所有的column
     *
     * @param tableName
     * @return
     */
    public List<ColumnSimpleInfo> getColumnsByTableName(String database, String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * from information_schema.columns WHERE table_schema = ")
                .append("'" + database + "'")
                .append(" and table_name = ")
                .append("'" + tableName + "'");
        final List<ColumnSimpleInfo> columns = new ArrayList<>();
        this.getJdbcTemplate().query(sql.toString(), (resultSet, i) -> {
            ColumnSimpleInfo temp = new ColumnSimpleInfo();
            temp.setTableName(resultSet.getString("table_name"));
            temp.setColumnName(resultSet.getString("column_name"));
            temp.setColumnType(resultSet.getString("data_type"));
            temp.setColumnComment(resultSet.getString("column_comment"));
            columns.add(temp);
            return null;
        });

        return columns;
    }

    public boolean createTable(String database, GenTableVO genTable) {
        String tableName = database + "." + genTable.getTableName();
        try {
            String sql = SqlUtils.generateCreateTableSql(tableName, genTable.getTableComment(), genTable.getGenTableColumn());
            this.getJdbcTemplate().execute(sql);
            return true;
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            return false;
        }

    }

    /**
     * 从本地load 数据到mysql
     *
     * @param filePath
     * @param tableName
     */
    public void importDataFromCsv(String filePath, String tableName) {

        StringBuilder sql = new StringBuilder();
        sql.append("load data infile ");
        sql.append("'" + filePath + "'");
        sql.append(" into table " + tableName);
        sql.append(" fields terminated by '\t'");
        this.getJdbcTemplate().execute(sql.toString());

    }

    /**
     * 删除db
     *
     * @param dbName
     */
    public void deleteDatabaseByDBName(String dbName) {
        String sql = "drop database if exists " + dbName;
        this.getJdbcTemplate().execute(sql);
    }

    public List<Map<String, Object>> getAllDataByTableName(String dbName, String tableName) {
        String sql = "select * from " + dbName + "." + tableName;
        return this.getJdbcTemplate().queryForList(sql);
    }

    public List<Map<String, Object>> getData(String dbName, String tableName, String where, String page, String limit) {
        String sql;

        if (StringUtils.isEmpty(where)) {
            sql = "select * from " + dbName + "." + tableName + " limit " + page + "," + limit;
        } else {
            sql = "select * from " + dbName + "." + tableName + " where " + where + " limit " + page + "," + limit;
        }
        return this.getJdbcTemplate().queryForList(sql);
    }
}
