package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.config.jdbcConfig.HiveJdbcBaseDaoImpl;
import cn.cnic.zhongtai.system.dao.HiveDao;
import cn.cnic.zhongtai.system.mapper.GenTableColumnMapper;
import cn.cnic.zhongtai.system.mapper.HiveTableMapper;
import cn.cnic.zhongtai.system.model.ColumnSimpleInfo;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.model.HiveTable;
import cn.cnic.zhongtai.system.model.TableInfo;
import cn.cnic.zhongtai.system.model.vo.HiveTableDataVo;
import cn.cnic.zhongtai.system.service.HiveService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Slf4j
@Service
public class HiveServiceImpl implements HiveService {

    @Autowired
    private HiveDao hiveDao;

    @Autowired
    private HiveJdbcBaseDaoImpl hiveJdbcBaseDaoImpl;

    @Resource
    private HiveTableMapper hiveTableMapper;

    @Resource
    private GenTableColumnMapper genTableColumnMapper;

    @Value("${syspara.HiveDbName}")
    private String HiveDbName;

    @Override
    public List<String> getAllTableName() {
        return hiveDao.showTables();
    }

    @Override
    public List<ColumnSimpleInfo> getColumnsByTableName(String tableName) {
        return hiveDao.getColumnsByTableName(tableName);
    }

    @Override
    public List<TableInfo> findAllTablesInfo() {
        return hiveDao.findAllTablesInfo();
    }

    @Override
    public List<Map<String, Object>> getDateByTableName(String tableName,int limit){

        if(StringUtils.isNotBlank(tableName)) {
            String sql = "SELECT * from  " + tableName;
            if (limit > 0) {
                sql = "SELECT * from  " + tableName + " limit " + limit;
            }
            List<Map<String, Object>> data = hiveJdbcBaseDaoImpl.getJdbcTemplate().queryForList(sql);
            return data == null ? Collections.emptyList() : data;
        } else {
            throw new RuntimeException("tableName为null");
        }
    }

    @Override
    public List<HiveTable> hiveTableListByTableName(String tableName){
        List<HiveTable> list = hiveTableMapper.hiveTableListByTableName(tableName);
        return !list.isEmpty() && null != list ? list : null;
    }


    @Override
    public void refreshHiveTable(){
        //查询库中的所有表
        String sql = " show tables in " +HiveDbName;
        //返回 List<Map<String, Object>>
        List<Map<String, Object>>  count =  hiveJdbcBaseDaoImpl.getJdbcTemplate().queryForList(sql);

        List<HiveTable> list = new ArrayList<HiveTable>();
        HiveTable hiveTable;
        if(count.size()>0 && !count.isEmpty() ){
            for (Map<String, Object> map : count) {
                for (String s : map.keySet()) {
                    hiveTable = new HiveTable();
                    //遍历校验是否有一致的表
                    hiveTable.setTableName(map.get(s)+"");
                    list.add(hiveTable);
                }
            }
            hiveTableMapper.insertHiveTableList(list);
        }
        log.info("成功插入hive表数据"+list.size()+"条");
    }

    @Override
    public void deleteByNames(String[] names) {
        for (String name : names) {
            //hive中drop table
            String hql = "";
            if (name.contains("\"")){
                hql = "drop table if exists " + name.replace("\"", "");
            } else {
                hql = "drop table if exists " + name;
            }
            hiveJdbcBaseDaoImpl.getJdbcTemplate().execute(hql);
        }
    }

    @Override
    public void deleteByName(String name) {
        //hive中drop table
        String hql = "";
        if (name.contains("\"")){
            hql = "drop table if exists " + name.replace("\"", "");
        }else {
            hql = "drop table if exists " + name;
        }
        hiveJdbcBaseDaoImpl.getJdbcTemplate().execute(hql);
    }

    @Override
    public void deleteAll() {
        List<String> allTableName = getAllTableName();
        for (String name : allTableName){
            deleteByName(name);
        }
    }


    @Override
    public JSONArray getColumnStatistics(String tableName,List<ColumnSimpleInfo> columnList){

        JSONArray arr = new  JSONArray();
        if(StringUtils.isNotBlank(tableName) && !columnList.isEmpty()){
            JSONArray json ;
            String columnName = "";
            for (ColumnSimpleInfo list : columnList) {
                json = new JSONArray();
                //拼接分析表
                columnName = "group_"+tableName+"_"+list.getColumnName();
                //查询语句,目前不限条数
                String sql =  "select * from  " + columnName +" limit 100";
                try {
                    List rows = hiveJdbcBaseDaoImpl.getJdbcTemplate().queryForList(sql);
                    JSONObject obj ;
                    if (!rows.isEmpty() && rows.size() > 0){
                        int hei = 0;
                        boolean flag = false;
                        //参数返回
                        for (int i = 0; i < rows.size(); i++) {
                            obj = new JSONObject();
                            Map userMap = (Map) rows.get(i);

                            Double rate = (Double)userMap.get(columnName + ".percentage");
                            NumberFormat num = NumberFormat.getPercentInstance();
                            String rates = num.format(rate);
                            String intRates = rates.substring(0, rates.length() - 1);

                            if(i == 0){
                                if (0 == Integer.parseInt(intRates) ){
                                    intRates = "1";
                                    flag = true;
                                }
                                hei = 100 / Integer.parseInt(intRates);
                            }
                            int height = Integer.parseInt(intRates) * hei;
                            if(0 ==height && flag){
                                obj.put("height",100);
                            }else{
                                obj.put("height",height);
                            }
                            obj.put("percentage",intRates);
                            obj.put("name",userMap.get(columnName + "." + list.getColumnName()));
                            json.add(obj);
                        }
                        arr.add(json);
                    }
                }catch (Exception e) {
                    log.error(tableName+" 表 分析数据查询失败"+e);
                    return arr;
                }

            }

        }
        log.info(tableName+"数据分析产出："+arr);
        return arr;
    }

    @Override
    public boolean validateTableNameExistByHive(String tableName){
        //全部转小写
        tableName =  tableName.toLowerCase();
        //查询库中的所有表
        String sql = " show tables in " +HiveDbName;
        //返回 List<Map<String, Object>>
        List<Map<String, Object>>  count =  hiveJdbcBaseDaoImpl.getJdbcTemplate().queryForList(sql);

        for (Map<String, Object> map : count) {
            for (String s : map.keySet()) {
                //遍历校验是否有一致的表
                if(tableName.equals(map.get(s))){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Integer getCountByHiveTableName(String hiveTableName) {
        String hql = "select count(*) from " + hiveTableName;
        List<Map<String, Object>> maps = hiveJdbcBaseDaoImpl.getJdbcTemplate().queryForList(hql);
        for (Map<String, Object> map : maps) {
            for (String s : map.keySet()) {
                //遍历校验是否有一致的表
                Object count = map.get(s);
                return ((Long) count).intValue();
            }
        }
        return -1;

    }


    @Override
    public List<GenTableColumn>  httpContentTest(String tableId){
     return genTableColumnMapper.selectModelColumnByTableId(tableId);
    }

    @Override
    public HiveTableDataVo getHiveTableInfo(String tableName) {

        String sql = "select *, count(1) cover() total from " + tableName + "limit 50";

        List<Map<String, Object>> data = hiveJdbcBaseDaoImpl.getJdbcTemplate().query(sql, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet resultSet, int index) throws SQLException {
                ResultSetMetaData rsMetaData = resultSet.getMetaData();
                List<String> var = new ArrayList<>();
                for (int i = 0, len = rsMetaData.getColumnCount(); i < len; i++) {
                    var.add(rsMetaData.getColumnName(i + 1));
                }
                List<String> columns = var.stream().filter(col -> !col.equals("total")).collect(Collectors.toList());

                Map<String, Object> rowMap = new HashMap<>();
                for (String col : columns) {
                    String colValue = resultSet.getString(col);
                    rowMap.put(col, colValue);
                }

                return rowMap;
            }
        });
        HiveTableDataVo hiveTableDataVo = new HiveTableDataVo();
        hiveTableDataVo.setTotal("0");
        if (!CollectionUtils.isEmpty(data)) {

            List<String> columnNames = data.get(0).keySet().stream().filter(var -> !var.equals("total")).collect(Collectors.toList());
            String total = (String) data.get(0).get("total");
            hiveTableDataVo.setTotal(total);
            hiveTableDataVo.setColumns(columnNames);
            hiveTableDataVo.setData(data);
        }
        return hiveTableDataVo;
    }
}
