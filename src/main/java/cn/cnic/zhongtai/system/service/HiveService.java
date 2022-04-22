package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.ColumnSimpleInfo;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.model.HiveTable;
import cn.cnic.zhongtai.system.model.TableInfo;
import cn.cnic.zhongtai.system.model.vo.HiveTableDataVo;
import com.alibaba.fastjson.JSONArray;

import java.util.List;
import java.util.Map;

public interface HiveService {

    List<String> getAllTableName();

    List<ColumnSimpleInfo> getColumnsByTableName(String tableName);

    List<TableInfo> findAllTablesInfo();

    /**
     * 根据表名查询hive中的数据
     * @param tableName
     * @param limit   需要查询的数量
     * @return
     */
    List<Map<String, Object>> getDateByTableName(String tableName, int limit);


    List<HiveTable> hiveTableListByTableName(String tableName);

    JSONArray getColumnStatistics(String tableName,List<ColumnSimpleInfo> columnList);

    void refreshHiveTable();


    void deleteByNames(String[] name);

    void deleteByName(String name);

    void deleteAll();

    /**
     * 校验hive表是否存在
     * @param tableName
     * @return
     */
    boolean validateTableNameExistByHive(String tableName);

    Integer getCountByHiveTableName(String hiveTableName);

    /**
     * http 暂时提供的测试接口
     * @param tableId
     * @return
     */
    List<GenTableColumn>   httpContentTest(String tableId);

    HiveTableDataVo getHiveTableInfo(String tableName);
}
