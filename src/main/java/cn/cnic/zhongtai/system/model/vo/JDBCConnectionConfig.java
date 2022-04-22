package cn.cnic.zhongtai.system.model.vo;

import cn.cnic.zhongtai.system.Constant;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static cn.cnic.zhongtai.system.Constant.HDFS_PATH_PRE;

@Data
public class JDBCConnectionConfig {

    private JSONObject setting;
    private JOB job;

    public static JDBCConnectionConfig generateConfig(ImportDataVo importDataVo) {

        Writer hdfsWriter = generateHDFSWriter(importDataVo.getDefaultFS(), importDataVo.getFileName(), importDataVo.getHdfsColumns(), importDataVo.getHdfsPath());

        Reader mysqlReader = generateRDBReader(importDataVo.getEngine(), importDataVo.getUserName(), importDataVo.getPassword(),
                new String[]{importDataVo.getTable()}, importDataVo.getColumn(), importDataVo.getSplitPk(),
                importDataVo.getJdbcUrl());

        Content content = new Content();
        content.setReader(mysqlReader);
        content.setWriter(hdfsWriter);

        JOB job = new JOB();
        JSONArray contents = new JSONArray();
        contents.set(0, content);
        job.setContent(contents);

        Setting setting = new Setting();
        Map<String, Object> speed = new HashMap<>();
        speed.put("byte", 123456);
        speed.put("channel", 1);
        setting.setSpeed(speed);

        job.setSetting(setting);

        JDBCConnectionConfig config = new JDBCConnectionConfig();
        config.setJob(job);

        return config;
    }

    private static Reader generateRDBReader(String engine, String username, String password, String[] table, String[] column, String splitPk, String jdbcUrl) {
        Reader mysqlReader = new Reader();
        mysqlReader.setName(engine);

        HashMap<String, Object> parameter = new HashMap<>();
        parameter.put("username", username);
        parameter.put("password", password);
        parameter.put("column", column);
        if (StringUtils.isNotEmpty(splitPk)) {
            parameter.put("splitPk", splitPk);
        }

        JSONArray connection = new JSONArray();
        Map<String, Object> connection0 = new HashMap<>();
        connection0.put("jdbcUrl", new String[]{jdbcUrl});
        connection0.put("table", table);
        connection.set(0, connection0);

        parameter.put("connection", connection);

        mysqlReader.setParameter(parameter);

        return mysqlReader;
    }

    private static Writer generateHDFSWriter(String defaultFS, String fileName, JSONArray column, String hdfsPath) {
        Writer hdfsWriter = new Writer();
        hdfsWriter.setName("hdfswriter");

        HashMap<String, Object> parameter = new HashMap<>();
        parameter.put("defaultFS", defaultFS);
        //parameter.put("fileType", "text");
        parameter.put("fileType", "orc");
        parameter.put("path", hdfsPath);
        parameter.put("fileName", fileName);
        parameter.put("column", column);
        parameter.put("writeMode", "append");
        parameter.put("fieldDelimiter", Constant.HDFS_DELIMITER);

        hdfsWriter.setParameter(parameter);

        return hdfsWriter;
    }


    @Data
    public static class JOB {
        private Setting setting;
        private JSONArray content;
    }

    @Data
    public static class Setting {
        private Map<String, Object> speed;
    }

    @Data
    public static class Content {
        private Reader reader;
        private Writer writer;
    }

    @Data
    public static class Reader {
        private String name;
        private Map<String, Object> parameter;
    }

    @Data
    public static class Writer {
        private String name;
        private Map<String, Object> parameter;
    }

}
