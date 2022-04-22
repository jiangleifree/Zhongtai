package cn.cnic.zhongtai.system.model;


import com.fasterxml.jackson.annotation.JsonIgnore;

public class ColumnSimpleInfo {

    @JsonIgnore
    private int columnId;
    private String columnName;
    private String columnType;
    private int columnLength;
    private String tableName;
    private String columnComment;

    public int getColumnLength() {
        return columnLength;
    }

    public ColumnSimpleInfo setColumnLength(int columnLength) {
        this.columnLength = columnLength;
        return this;
    }

    public String getColumnComment() {
        return columnComment;
    }

    public ColumnSimpleInfo setColumnComment(String columnComment) {
        this.columnComment = columnComment;
        return this;
    }

    public String getTableName() {
        return tableName;
    }

    public ColumnSimpleInfo setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public int getColumnId() {
        return columnId;
    }

    public ColumnSimpleInfo setColumnId(int columnId) {
        this.columnId = columnId;
        return this;
    }

    public String getColumnName() {
        return columnName;
    }

    public ColumnSimpleInfo setColumnName(String columnName) {
        this.columnName = columnName;
        return this;
    }

    public String getColumnType() {
        return columnType;
    }

    public ColumnSimpleInfo setColumnType(String columnType) {
        this.columnType = columnType;
        return this;
    }

}
