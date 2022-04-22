package cn.cnic.zhongtai.system.mapper;


import cn.cnic.zhongtai.system.config.jdbcConfig.HiveJdbcBaseDaoImpl;
import cn.cnic.zhongtai.system.config.jdbcConfig.MysqlMainJdbcBaseDaoImpl;
import org.springframework.stereotype.Repository;

/**
 * -测试jdbc连接
 * @author sixmonth
 * @Date 2019年5月20日
 *
 */
@Repository
public class TestDao extends HiveJdbcBaseDaoImpl {

    /**
     * -测试类
     * @return
     */
    /*public String test() {
        String sql = "SELECT table_name from gen_table where table_id = '7';";
        String param = this.getJdbcTemplate().queryForObject(sql, String.class);
        return param ;
    }*/
    /**
     * 测试获取hive数据库数据信息
     * @return
     */
    public String test() {
        String sql = "SELECT name from test.people limit 1";
        String param = this.getJdbcTemplate().queryForObject(sql,String.class);
        return param;
    }

}