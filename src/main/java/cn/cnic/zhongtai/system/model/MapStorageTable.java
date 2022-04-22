package cn.cnic.zhongtai.system.model;

import lombok.Data;

/**
 * 图库中的表
 */
@Data
public class MapStorageTable {

    private String tableName;
    //node和relationship
    private String type;
    //hive中对应的库名
    private String hiveDBName;
    //选中的列
    private String columns;
}
