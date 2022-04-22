package cn.cnic.zhongtai.system.model.vo;


import java.util.ArrayList;
import java.util.List;

public class DataClearVO {

    private String tableName;

    private String newTableName;

    //类型：清洗，分词，生成中间表
    private String type;


    //虚拟目录的id
    private int pid;
    //虚拟目录的name
    private String dataMappingName;

    private List<String> tableColumn;

    private List<DataOperationVO> dataOperation = new ArrayList<>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getTableColumn() {
        return tableColumn;
    }

    public void setTableColumn(List<String> tableColumn) {
        this.tableColumn = tableColumn;
    }

    public List<DataOperationVO> getDataOperation() {
        return dataOperation;
    }

    public void setDataOperation(List<DataOperationVO> dataOperation) {
        this.dataOperation = dataOperation;
    }

    public String getNewTableName() {
        return newTableName;
    }

    public void setNewTableName(String newTableName) {
        this.newTableName = newTableName;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getDataMappingName() {
        return dataMappingName;
    }

    public void setDataMappingName(String dataMappingName) {
        this.dataMappingName = dataMappingName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
