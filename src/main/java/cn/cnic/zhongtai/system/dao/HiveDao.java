package cn.cnic.zhongtai.system.dao;

import cn.cnic.zhongtai.system.config.jdbcConfig.HiveJdbcBaseDaoImpl;
import cn.cnic.zhongtai.system.model.ColumnSimpleInfo;
import cn.cnic.zhongtai.system.model.GenTable;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.model.TableInfo;
import cn.cnic.zhongtai.utils.CommonUtils;
import cn.cnic.zhongtai.utils.SqlUtils;
import cn.cnic.zhongtai.utils.UnixUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
@Slf4j
public class HiveDao extends HiveJdbcBaseDaoImpl {

    private static final String EXPORT_PATH_PREX = "/data/tobaccoZhongtai/export/";

    /**
     * show tables
     *
     * @return
     */
    public List<String> showTables() {
        String sql = "show tables";
        final List<String> tables = new ArrayList<>();

        this.getJdbcTemplate().query(sql, resultSet -> {
            tables.add(resultSet.getString(1));
        });
        return tables;
    }

    public String findTableComment(String tableName) {
        String sql = "show tblproperties " + tableName;
        String comment = "";
        List<Map<String, Object>> maps = this.getJdbcTemplate().queryForList(sql);
        boolean flag = false;
        for (Map<String, Object> map : maps) {
            for (String key : map.keySet()) {
                if (flag == true) {
                    comment = (String) map.get(key);
                    return comment;
                }
                if (map.get(key).equals("comment")) {
                    flag = true;
                }
            }
        }
        return comment;
    }

    /**
     * query all tablesinfo from default database
     *
     * @return
     */
    public List<TableInfo> findAllTablesInfo() {
        List<String> tables = this.showTables();
        List<TableInfo> tableInfos = new ArrayList<>();
        TableInfo temp = null;
        for (String table : tables) {
            List<ColumnSimpleInfo> columnSimpleInfos = this.getColumnsByTableName(table);

            temp = new TableInfo();
            temp.setTableComment(this.findTableComment(table));
            temp.setTableName(table);
            temp.setColumnSampleInfos(columnSimpleInfos);
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
    public List<ColumnSimpleInfo> getColumnsByTableName(String tableName) {
        String sql = "describe " + tableName;

        final List<ColumnSimpleInfo> columns = new ArrayList<>();
        this.getJdbcTemplate().query(sql, (resultSet, i) -> {
            columns.add(new ColumnSimpleInfo().setColumnName(resultSet.getString(1))
                    .setColumnType(resultSet.getString(2))
                    .setColumnComment(resultSet.getString(3)));
            return null;
        });

        return columns;
    }

    /**
     * hive 导出查询数据到csv文件
     *
     * @param tableName
     * @param columns
     * @return 返回的是数据所在路径
     */
    public String exportHiveDataToCsv(String tableName, String columns) {

        String sql = SqlUtils.generateGetListSql(tableName, columns);
        String tempCsvName = tableName + CommonUtils.getUUID32();
        String filePath = EXPORT_PATH_PREX + tempCsvName + ".csv";
        String command = SqlUtils.generateHiveExportDataToCsvCommand(filePath, sql);
        log.error("command");
        log.error(command);

        UnixUtils.run(new String[]{command}, false);

        return filePath;
    }

    @Deprecated
    public boolean createTables(List<GenTable> genTables) {
        if (CollectionUtils.isEmpty(genTables)) {
            throw new IllegalArgumentException("tableName and columns is must not empty!");
        }
        List<Boolean> booleans = genTables.stream().map(genTable -> createTable(genTable)).collect(Collectors.toList());

        return !booleans.contains(false);
    }


    @Deprecated
    public boolean createTable(GenTable genTable) {

        List<GenTableColumn> genTableColumn = genTable.getGenTableColumn();

        final StringBuilder sql = new StringBuilder();
        sql.append("create table if not exists ");
        sql.append(genTable.getTableName());
        sql.append("(");

        IntStream.range(0, genTableColumn.size())
                .forEach(index -> {
                    if (index < genTableColumn.size() - 1) {
                        sql.append(genTableColumn.get(index).getColumnName() + " " + genTableColumn.get(index).getColumnType() + ",");
                    } else {
                        sql.append(genTableColumn.get(index).getColumnName() + " " + genTableColumn.get(index).getColumnType() + ")");
                    }
                });

        sql.append("ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' "); // 定义分隔符
        sql.append("STORED AS TEXTFILE"); // 作为文本存储

        log.info(sql.toString());

        try {
            this.getJdbcTemplate().execute(sql.toString());
            return true;
        } catch (Exception e) {
            return false;
        }


    }


}
