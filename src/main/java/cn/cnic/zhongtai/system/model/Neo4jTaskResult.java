package cn.cnic.zhongtai.system.model;

import lombok.Data;

@Data
public class Neo4jTaskResult {
    boolean createHiveTable;
    boolean createHdfsDir;
    boolean importData;
}
