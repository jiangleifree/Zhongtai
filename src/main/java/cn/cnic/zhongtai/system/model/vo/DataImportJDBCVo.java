package cn.cnic.zhongtai.system.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class DataImportJDBCVo {

    private String jdbcUrl;
    private String boltUrl;
    private String userName;
    private String password;
    private String modelTypeName; //数据接入名称
    private List<Table> tables;
    private List<Table> labels;
    private List<Table> relations;
    private String pid;


    @Data
    public static class Table{
        private String tableName;
        private String importType;
        private String hiveTableName;
        private List<String> columns;
    }

}
