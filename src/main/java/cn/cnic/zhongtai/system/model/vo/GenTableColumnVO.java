package cn.cnic.zhongtai.system.model.vo;

import lombok.Data;

@Data
public class GenTableColumnVO {

    private String columnName;
    //来自hive的哪张表
    private String fromTable;

    //字段需要重新命名存到数据库
    private String realColumnName;
    private String columnComment;

    private String columnType;

    private String columnLength;


}