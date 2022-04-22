package cn.cnic.zhongtai.system.config.druid.mysql;

import java.sql.SQLException;

import javax.sql.DataSource;

import cn.cnic.zhongtai.system.config.DataSourceCommonProperties;
import cn.cnic.zhongtai.system.config.DataSourceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * -配置mysql主数据源，mysql连接默认主数据源
 * @author wdd
 * @Date 2019年11月04日
 *
 */
@Configuration
@EnableConfigurationProperties({DataSourceProperties.class, DataSourceCommonProperties.class})//将配置类注入到bean容器，使ConfigurationProperties注解类生效
public class MysqlMainDruidConfig {

    private static Logger logger = LoggerFactory.getLogger(MysqlMainDruidConfig.class);

    @Autowired
    private DataSourceProperties dataSourceProperties;

    @Autowired
    private DataSourceCommonProperties dataSourceCommonProperties;

    @Primary //标明为主数据源，只能标识一个主数据源，mybatis连接默认主数据源
    @Bean("mysqlDruidDataSource") //新建bean实例
    @Qualifier("mysqlDruidDataSource")//标识
    public DataSource dataSource(){
        DruidDataSource datasource = new DruidDataSource();

        //配置数据源属性
        datasource.setUrl(dataSourceProperties.getMysqlMain().get("url"));
        datasource.setUsername(dataSourceProperties.getMysqlMain().get("username"));
        datasource.setPassword(dataSourceProperties.getMysqlMain().get("password"));
        datasource.setDriverClassName(dataSourceProperties.getMysqlMain().get("driver-class-name"));

        //配置统一属性
        datasource.setInitialSize(dataSourceCommonProperties.getInitialSize());
        datasource.setMinIdle(dataSourceCommonProperties.getMinIdle());
        datasource.setMaxActive(dataSourceCommonProperties.getMaxActive());
        datasource.setMaxWait(dataSourceCommonProperties.getMaxWait());
        datasource.setTimeBetweenEvictionRunsMillis(dataSourceCommonProperties.getTimeBetweenEvictionRunsMillis());
        datasource.setMinEvictableIdleTimeMillis(dataSourceCommonProperties.getMinEvictableIdleTimeMillis());
        datasource.setValidationQuery(dataSourceCommonProperties.getValidationQuery());
        datasource.setTestWhileIdle(dataSourceCommonProperties.isTestWhileIdle());
        datasource.setTestOnBorrow(dataSourceCommonProperties.isTestOnBorrow());
        datasource.setTestOnReturn(dataSourceCommonProperties.isTestOnReturn());
        datasource.setPoolPreparedStatements(dataSourceCommonProperties.isPoolPreparedStatements());
        try {
            datasource.setFilters(dataSourceCommonProperties.getFilters());
        } catch (SQLException e) {
            logger.error("Druid configuration initialization filter error.", e);
        }
        return datasource;
    }

    /**
     * 多数据源需要配置事务管理
     * @param dataSource
     * @return
     */
    @Primary
    @Bean(name = "mysqlShardTransactionManager")
    public PlatformTransactionManager txManager(@Qualifier("mysqlDruidDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

}