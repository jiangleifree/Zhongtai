package cn.cnic.zhongtai.system.model;

import lombok.Data;

import java.util.List;

/**
 * 数据接入 数据清洗  需要的参数
 */
@Data
public class SparkParam {

    /**
     * 文件类型  csv/json
     */
    private String suffix;
    /**
     * hive中对应的表名
     */
    private String hiveTableName;
    /**
     * 数据导入方式
     * 追加/覆盖/新建  0/1/2
     */
    private String type;
    /**
     * 对应的表结构
     */
    private List<String> column;
    /**
     * hdfs存放的路径
     */
    private String hdfs;
    /**
     * 分隔符号
     */
    private String sign;
    /**
     *
     */
    private String source;

    private String pid;
}
