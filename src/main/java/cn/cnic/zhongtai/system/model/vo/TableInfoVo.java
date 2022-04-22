package cn.cnic.zhongtai.system.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class TableInfoVo {
    @JsonIgnore
    private int tableId;
    private String tableName;
    private String topicName;
    private List<String> columns;

    public String getTopicName() {
        return topicName;
    }

    public TableInfoVo setTopicName(String topicName) {
        this.topicName = topicName;
        return this;
    }

    public int getTableId() {
        return tableId;
    }

    public TableInfoVo setTableId(int tableId) {
        this.tableId = tableId;
        return this;
    }

    public String getTableName() {
        return tableName;
    }

    public TableInfoVo setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }



    public List<String> getColumns() {
        return columns;
    }

    public TableInfoVo setColumns(List<String> columns) {
        this.columns = columns;
        return this;
    }


}
