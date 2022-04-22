package cn.cnic.zhongtai.utils;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.model.vo.GenTableColumnVO;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Component
@Slf4j
public class SqlUtils {


    @Value("${syspara.HiveDbName}")
    private String HiveDbName;

    private static Logger logger = LoggerUtil.getLogger();

    /**
     * uuid(32位的)
     *
     * @return
     */
    public static String getUUID32() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    /**
     * str(给字符串加单引)
     *
     * @param str
     * @return
     */
    public static String addSqlStr(String str) {
        if (StringUtils.isNotBlank(str)) {
            return "'" + str + "' ";
        } else {
            return "'' ";
        }
    }

    /**
     * 生成hive导出csv语句
     *
     * @return
     */
    public static String generateHiveExportDataToCsvCommand(String dataPath, String selectSql) {
        StringBuilder command = new StringBuilder();
        command.append("hive -e ");
        command.append("\"" + selectSql + "\"");
        command.append(" >> ");
        command.append(dataPath);
        logger.error(command.toString());
        return command.toString();
    }

    public static final Map<String, String> COLUMN_TYPE_MAP = new HashMap<>();

    static {
        COLUMN_TYPE_MAP.put("string", "text");
        COLUMN_TYPE_MAP.put("int", "int");
        COLUMN_TYPE_MAP.put("bigint", "bigint");
        COLUMN_TYPE_MAP.put("tinyint", "tinyint");
        COLUMN_TYPE_MAP.put("char", "char");
        COLUMN_TYPE_MAP.put("mediumint", "mediumint");
        COLUMN_TYPE_MAP.put("smallint", "smallint");
        COLUMN_TYPE_MAP.put("date", "date");
        COLUMN_TYPE_MAP.put("datetime", "date");
        COLUMN_TYPE_MAP.put("time", "date");
    }

    /**
     * mysql建表语句
     * @param tableName 表名
     * @param table_comment 表描述
     * @param columns 字段信息
     * @return
     */
    public static String generateCreateTableSql(String tableName, String table_comment, List<GenTableColumnVO> columns) {

        for (int i = 0; i < columns.size(); i++) {
            GenTableColumnVO col = columns.get(i);
            if (StringUtils.isBlank(col.getColumnLength()) || "null".equals(col.getColumnLength())) { //如果没有设置长度 设置默认长度为256
                col.setColumnLength("256");
            }
            if (StringUtils.isBlank(col.getColumnType()) || "null".equals(col.getColumnType())) {  //设置默认类型为string
                col.setColumnType("string");
            }

            if (!COLUMN_TYPE_MAP.containsKey(col.getColumnType())) { //如果类型不在涉及的范围内, 统一转为string
                logger.error("column type not correct, set to string");
                col.setColumnType("string");
            }

        }

        String str = "CREATE TABLE ";
        if (StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(table_comment)) {
            str += "" + tableName + " ( ";
        } else {
            throw new RuntimeException("tableName or table_comment is null");
        }
        if (!CollectionUtils.isEmpty(columns)) {
            for (GenTableColumnVO column : columns) {
                //str += "" + column.getRealColumnName() + " \t " + COLUMN_TYPE_MAP.get(column.getColumnType()) +
                //        "(" + column.getColumnLength() + ")" +
                //        " COMMENT '" + column.getColumnComment() + "' DEFAULT NULL " + ",";
                str += "`" + column.getColumnName() + "` \t " + generateColumnSting(column.getColumnType(), column.getColumnLength(),
                        column.getColumnComment(), "");

            }
            //去除最后一个逗号
            str = str.substring(0, str.length() - 1);

        }
        str += ") ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COMMENT = '" + table_comment + "';";
        return str;
    }

    private static String generateColumnSting(String type, String length, String comment, String default_) {
        String ret = "";
        if (type.equals("date")) {
            ret += COLUMN_TYPE_MAP.get(type);
        } else if (type.equals("datetime")) {
            ret += COLUMN_TYPE_MAP.get(type);
        } else if (type.equals("time")) {
            ret += COLUMN_TYPE_MAP.get(type);
        } else if (type.equals("string")){
            ret += COLUMN_TYPE_MAP.get(type);
        } else {
            ret += COLUMN_TYPE_MAP.get(type) + "(" + length + ")";
        }
        ret += " comment '" + comment + "'";
        ret += " default null,";
        return ret;
    }


    /**
     * 创建查询sql
     *
     * @param tableName
     * @param columns
     * @return TODO 这里一次查询所有数据  后面根据需要分批查询
     */
    public static String generateGetListSql(String tableName, String columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append(columns);
      //  sql.append(" from tobaccoZhongtai." + tableName);
        sql.append(" from " + tableName);
        return sql.toString();
    }

    /**
     * str(给字符串加单引,并替换字符串中的单引为双单引)
     *
     * @param str
     * @return
     */
    public static String addSqlStrAndReplace(String str) {
        if (StringUtils.isNotBlank(str)) {
            return "'" + replaceString(str) + "' ";
        } else {
            return "'' ";
        }
    }

    /**
     * str(替换字符串中的单引为双单引)
     *
     * @param str
     * @return
     */
    public static String replaceString(String str) {
        if (null != str) {
            str = str.replace("'", "''");
        }
        return str;
    }

    /**
     * 数组转换为,分割的字符串
     * @param strArray
     * @return
     */
    public static String strArrayToStr(String[] strArray) {
        String str = "";
        if (null != strArray && strArray.length > 0) {
            for (int i = 0; i < strArray.length; i++) {
                if (StringUtils.isNotBlank(strArray[i])) {
                    str += strArray[i];
                    if (i < strArray.length - 1) {
                        str += ",";
                    }
                }
            }
        }
        return str;
    }

    /**
     * 创建mysql的表sql语句
     * @param tableName 表名
     * @param table_comment 表描述
     * @param GenTableColumnlist 表字段信息
     * @return
     */
    public static String createTableByMysql( String tableName, String table_comment, List<GenTableColumn> GenTableColumnlist) {
        String str = "CREATE TABLE ";
        if(StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(table_comment)){
              str += ""+tableName+" ( ";
        }
        if(!GenTableColumnlist.isEmpty() && null != GenTableColumnlist){
            for (GenTableColumn genTableColumn: GenTableColumnlist) {
                str += ""+genTableColumn.getColumnName()+" \t " + genTableColumn.getColumnType()+"("+""+genTableColumn.getColumnLength()+") DEFAULT NULL COMMENT '" + genTableColumn.getColumnComment() +"'," ;
            }
            //去除最后一个逗号
            str = str.substring(0,str.length()-1);

        }
        str += ") ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT = '" + table_comment +"';";
        return str;
    }


    /**
     * hive建表语句
     * @param tableName
     * @param table_comment
     * @param GenTableColumnlist
     * @return
     */
    public static String createTableByHiveSql( String tableName, String table_comment, List<GenTableColumn> GenTableColumnlist) {
        String str = "CREATE TABLE IF NOT EXISTS "    ;
        if(StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(table_comment)){
           // str += "tobaccozhongtai."+tableName+" ( ";
            str += tableName+" ( ";
        }
        if(!GenTableColumnlist.isEmpty() && null != GenTableColumnlist){
            for (GenTableColumn genTableColumn: GenTableColumnlist) {
                str += " "+genTableColumn.getColumnName()+" " + genTableColumn.getColumnType()+" COMMENT '" + genTableColumn.getColumnComment() +"'," ;
            }
            //去除最后一个逗号
            str = str.substring(0,str.length()-1);
        }
        str += ") COMMENT '" + table_comment +"'   ";
        str+= " row format delimited" ;
        str += " fields terminated by ',' ";
        str += " stored as textfile ";

        return str;
    }

    /**
     * hive建表语句 ORC
     * @param tableName
     * @param table_comment
     * @param columns
     * @return
     */
    public static String generateCreateHiveTableSqlByORC( String tableName,
                                                          String table_comment,
                                                          List<String> columns,
                                                          String hdfsPath) {
        String str = "CREATE EXTERNAL TABLE ";
        str += "`" + tableName + "`( \n";
        if(!columns.isEmpty() && null != columns){
            for (String columnName: columns) {
                str += " `" + columnName + "` string COMMENT '',";
            }
            //去除最后一个逗号
            str = str.substring(0,str.length()-1);
        }
        str += ") COMMENT '" + table_comment +"'";
        //str += " ROW FORMAT SERDE  'org.apache.hadoop.hive.ql.io.orc.OrcSerde' ";
        str += " ROW FORMAT DELIMITED ";
        str += " FIELDS TERMINATED BY '" + Constant.HDFS_DELIMITER + "'";
        str += " STORED AS INPUTFORMAT  'org.apache.hadoop.hive.ql.io.orc.OrcInputFormat' ";
        str += " OUTPUTFORMAT  'org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat' ";
        str += " LOCATION '" + hdfsPath + "'";
        return str;
    }

    /**
     * hive建表语句 ORC
     * @param tableName
     * @param table_comment
     * @param columns
     * @return
     */
    public static String generateCreateHiveTableSqlByORC2( String tableName,
                                                          String table_comment,
                                                          List<String> columns,
                                                          String hdfsPath) {
        String str = "CREATE EXTERNAL TABLE ";
        str += "`" + tableName + "`( \n";
        if(!columns.isEmpty() && null != columns){
            for (String columnName: columns) {
                str += " `" + columnName + "` string COMMENT '',";
            }
            //去除最后一个逗号
            str = str.substring(0,str.length()-1);
        }
        str += ") COMMENT '" + table_comment +"'";
        //str += " ROW FORMAT SERDE  'org.apache.hadoop.hive.ql.io.orc.OrcSerde' ";
        str += " ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.orc.OrcSerde'";
        //str += " FIELDS TERMINATED BY '" + Constant.HDFS_DELIMITER + "'";
        str += " STORED AS INPUTFORMAT  'org.apache.hadoop.hive.ql.io.orc.OrcInputFormat' ";
        str += " OUTPUTFORMAT  'org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat' ";
        str += " LOCATION '" + hdfsPath + "'";
        return str;
    }

    /**
     *
     * 创建插入sql 格式为：insert into ** (**,**) values (**,**),(**,**);
     * @param tableName 表名
     * @param records 解析出的JSONArray数据
     * @param GenTableColumnlist 字段信息
     * @return
     */
    public static String createBatchInsertSql(String tableName,JSONArray records,   List<GenTableColumn> GenTableColumnlist) {
        StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO ");

        if(StringUtils.isNotBlank(tableName) ){
            sb.append(""+tableName+" ( ");
        }
        for (GenTableColumn genTableColumn: GenTableColumnlist) {
            sb.append(""+genTableColumn.getColumnName()+",");
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append(") VALUES ");
        logger.info(sb.toString());

        for(int i=0;i<records.size();i++){ //item2为json数组，数组中为对象
            JSONObject jsonItem=(JSONObject)records.get(i);
            sb.append("(");
            for (GenTableColumn genTableColumn: GenTableColumnlist) {
                String columnName = genTableColumn.getColumnName();
                String vluee =jsonItem.getString(columnName);
                //判断是否能拿到这个值,拿不到直接返回,表示文件内容与数据库字段不符或者数据有问题
                if(StringUtils.isBlank(vluee) || "".equals(vluee)){
                    StringBuffer sbb = new StringBuffer("-1");
                    return sbb.toString();
                }
                sb.append("'"+vluee+"',");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("),");

        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }


    /**
     * 插入sql语句 by csv 文件
     * @param tableName 表名
     * @param r csv内容
     * @param GenTableColumnlist 表字段信息
     * @return
     * @throws IOException
     */
    public static String createBatchInsertSqlByCsvFile(String tableName,CsvReader r, List<GenTableColumn> GenTableColumnlist) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO ");

        if(StringUtils.isNotBlank(tableName) ){
            sb.append(""+tableName+" ( ");
        }
        for (GenTableColumn genTableColumn: GenTableColumnlist) {
            sb.append(""+genTableColumn.getColumnName()+",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(") VALUES ");

        while (r.readRecord()) {
            sb.append("(");
            System.out.println(r.get("table_id"));
            for (GenTableColumn genTableColumn: GenTableColumnlist) {
                String columnName = genTableColumn.getColumnName();
                String vluee =r.get(columnName);
                //判断是否能拿到这个值,拿不到直接返回,表示文件内容与数据库字段不符或者数据有问题
                if(StringUtils.isBlank(vluee) || "".equals(vluee)){
                    StringBuffer sbb = new StringBuffer("-1");
                    return sbb.toString();
                }
                sb.append("'"+vluee+"',");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("),");
        }
        sb.deleteCharAt(sb.length() - 1);
        logger.info(sb.toString());
        return sb.toString();

    }


    /**
     * 创建外部表
     * 通过hdfs方式进行拆入数据, 使用逗号分隔
     * @param tableName
     * @param table_comment
     * @param GenTableColumnlist
     * @return
     */
    public String createTableToHiveByHdfs( String tableName, String table_comment, List<GenTableColumn> GenTableColumnlist,String hdfsUrl) {
        return createTableToHiveByHdfs(tableName, table_comment, GenTableColumnlist, hdfsUrl, ",");
    }

    /**
     * 创建外部表
     * 通过hdfs方式进行拆入数据,
     * @param tableName
     * @param table_comment
     * @param GenTableColumnlist
     * @return
     */
    public String createTableToHiveByHdfs( String tableName, String table_comment, List<GenTableColumn> GenTableColumnlist,String hdfsUrl, String delimiter) {
        String str = "create external table ";
        if(StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(table_comment)){
            str += HiveDbName+"."+tableName+" ( \n";
        }
        if(!GenTableColumnlist.isEmpty() && null != GenTableColumnlist){
            for (GenTableColumn genTableColumn: GenTableColumnlist) {
                str += " "+genTableColumn.getColumnName()+" " + genTableColumn.getColumnType()+"\n ," ;
            }
            //去除最后一个逗号
            str = str.substring(0,str.length()-1);
        }
        str += ") COMMENT '" + table_comment +"'   ";
        str+= " row format delimited" ;
        str += " fields terminated by";
        str += " '" + delimiter + "'";
        str += " stored as textfile ";
        str +=  "location '"+hdfsUrl+"' ";
        return str;
    }

}
