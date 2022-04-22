package cn.cnic.zhongtai.system.model.vo;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;


@Data
public class ImportDataVo {
    // mysql postgresql oracle sqlserver
    private String engine;
    private String userName;
    private String password;
    //"jdbc:mysql://bad_ip:3306/database",
    private String jdbcUrl;
    private String table;
    private String[] column;
    private String splitPk;

    private String defaultFS;
    private String fileName;
    private JSONArray hdfsColumns;
    private String hdfsPath;


}
