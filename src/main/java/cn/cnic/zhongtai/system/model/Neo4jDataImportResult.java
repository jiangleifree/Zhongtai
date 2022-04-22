package cn.cnic.zhongtai.system.model;

import lombok.Data;

import java.util.List;

@Data
public class Neo4jDataImportResult {
    private String tableName;
    private String hiveTableName;
    private List<String> columns;
    private String importType;
    private String hdfsUrl;
    private String result;
    private String errMsg;
    private String msg;
    private String type;
}
