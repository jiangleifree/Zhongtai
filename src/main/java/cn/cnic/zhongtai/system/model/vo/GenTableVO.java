package cn.cnic.zhongtai.system.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class GenTableVO {

    private Long tableId;

    private String tableName;

    private String tableComment;

    private List<GenTableColumnVO> genTableColumn;
    //过滤条件
    private String where;
    //关联条件
    private String associatedConditions;

    //数据分割需要的字段
    private String split;
    private String splitColumnName;
    private String pid;
}
