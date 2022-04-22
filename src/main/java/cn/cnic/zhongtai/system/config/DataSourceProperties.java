package cn.cnic.zhongtai.system.config;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * -统一属性控制类，获取配置文件属性
 * @author wdd
 * @Date 2019年11月04日
 *
 */
@ConfigurationProperties(prefix = DataSourceProperties.DS, ignoreUnknownFields = false)
public class DataSourceProperties {
    final static String DS = "spring.datasource";

    private Map<String,String> mysqlMain;

    private Map<String,String> hive;

    private Map<String,String> commonConfig;


    public static String getDS() {
        return DS;
    }

    public Map<String, String> getMysqlMain() {
        return mysqlMain;
    }

    public void setMysqlMain(Map<String, String> mysqlMain) {
        this.mysqlMain = mysqlMain;
    }

    public Map<String, String> getHive() {
        return hive;
    }

    public void setHive(Map<String, String> hive) {
        this.hive = hive;
    }

    public Map<String, String> getCommonConfig() {
        return commonConfig;
    }

    public void setCommonConfig(Map<String, String> commonConfig) {
        this.commonConfig = commonConfig;
    }
}