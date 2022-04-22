package cn.cnic.zhongtai.system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;


import java.util.List;

@Getter
@Setter
public class TableInfo {

    @JsonIgnore
    private int tableId;
    private String tableName;
    private String topicName;
    private String columns;
    private List<ColumnSimpleInfo> columnSampleInfos;
    private String tableComment;

}
