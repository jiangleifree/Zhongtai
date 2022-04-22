package cn.cnic.zhongtai.utils;

import cn.cnic.zhongtai.system.Constant;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

@Slf4j
public class DBUtil {

    /**
     * Get direct JDBC connection
     * <p/>
     * if connecting failed, try to connect for MAX_TRY_TIMES times
     * <p/>
     * NOTE: In DataX, we don't need connection pool in fact
     */
    public static Connection getConnection(final DataBaseType dataBaseType,
                                           final String jdbcUrl, final String username, final String password, final int retryTimes) {

        return getConnection(dataBaseType, jdbcUrl, username, password, String.valueOf(Constant.SOCKET_TIMEOUT_INSECOND * 1000), retryTimes);
    }

    /**
     * @param dataBaseType
     * @param jdbcUrl
     * @param username
     * @param password
     * @param socketTimeout 设置socketTimeout，单位ms，String类型
     * @return
     */
    public static Connection getConnection(final DataBaseType dataBaseType,
                                           final String jdbcUrl, final String username, final String password, final String socketTimeout, final int retryTimes) {

        try {
            return RetryUtil.executeWithRetry(new Callable<Connection>() {
                @Override
                public Connection call() throws Exception {
                    return DBUtil.connect(dataBaseType, jdbcUrl, username,
                            password, socketTimeout);
                }
            }, retryTimes, 1000L, true);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("数据库连接失败. 因为根据您配置的连接信息:%s获取数据库连接失败. 请检查您的配置并作出修改.", jdbcUrl), e);
        }
    }

    private static synchronized Connection connect(DataBaseType dataBaseType,
                                                   String url, String user, String pass, String socketTimeout) {

        //ob10的处理
        if (url.startsWith(Constant.OB10_SPLIT_STRING) && dataBaseType == DataBaseType.MySql) {
            String[] ss = url.split(Constant.OB10_SPLIT_STRING_PATTERN);
            if (ss.length != 3) {
                throw new RuntimeException("JDBC OB10格式错误，请联系askdatax");
            }
            log.info("this is ob1_0 jdbc url.");
            user = ss[1].trim() + ":" + user;
            url = ss[2];
            log.info("this is ob1_0 jdbc url. user=" + user + " :url=" + url);
        }

        Properties prop = new Properties();
        prop.put("user", user);
        prop.put("password", pass);

        if (dataBaseType == DataBaseType.Oracle) {
            //oracle.net.READ_TIMEOUT for jdbc versions < 10.1.0.5 oracle.jdbc.ReadTimeout for jdbc versions >=10.1.0.5
            // unit ms
            prop.put("oracle.jdbc.ReadTimeout", socketTimeout);
        }

        return connect(dataBaseType, url, prop);
    }

    private static synchronized Connection connect(DataBaseType dataBaseType,
                                                   String url, Properties prop) {
        try {
            Class.forName(dataBaseType.getDriverClassName());
            DriverManager.setLoginTimeout(Constant.TIMEOUT_SECONDS);
            return DriverManager.getConnection(url, prop);
        } catch (Exception e) {
            e.printStackTrace();
            String errMsg = e.getMessage();
            /*if (errMsg.startsWith("Access denied for user")) {
                throw new RuntimeException("username or password not correct");
            } else if (errMsg.startsWith("Unknown database")) {
                throw new RuntimeException("database not exist");
            } else if (errMsg.startsWith("Communications link failure")) {
                throw new RuntimeException("connect fail, please check host or port");
            }*/
            throw new RuntimeException(e);
        }
    }

    /**
     * a wrapped method to execute select-like sql statement .
     *
     * @param conn Database connection .
     * @param sql  sql statement to be executed
     * @return a {@link ResultSet}
     * @throws SQLException if occurs SQLException.
     */
    public static ResultSet query(Connection conn, String sql, int fetchSize)
            throws SQLException {
        // 默认3600 s 的query Timeout
        return query(conn, sql, fetchSize, Constant.SOCKET_TIMEOUT_INSECOND);
    }


    /**
     * a wrapped method to execute select-like sql statement .
     *
     * @param conn         Database connection .
     * @param sql          sql statement to be executed
     * @param fetchSize
     * @param queryTimeout unit:second
     * @return
     * @throws SQLException
     */
    public static ResultSet query(Connection conn, String sql, int fetchSize, int queryTimeout)
            throws SQLException {
        // make sure autocommit is off
        conn.setAutoCommit(false);
        Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(fetchSize);
        stmt.setQueryTimeout(queryTimeout);
        return query(stmt, sql);
    }

    /**
     * a wrapped method to execute select-like sql statement .
     *
     * @param stmt {@link Statement}
     * @param sql  sql statement to be executed
     * @return a {@link ResultSet}
     * @throws SQLException if occurs SQLException.
     */
    public static ResultSet query(Statement stmt, String sql)
            throws SQLException {
        return stmt.executeQuery(sql);
    }

    public static List<String> getTables(DataBaseType dataBaseType,
                                         String dbName, Connection conn) {

        ResultSet rs = null;
        String sql = null;
        List<String> tables = new ArrayList<>();

        if (dataBaseType == DataBaseType.MySql) {
            sql = "select table_name from information_schema.tables where table_schema='" + dbName + "'";
        } else if (dataBaseType == DataBaseType.PostgreSQL) {
            sql = "select tablename from pg_tables  where schemaname = 'public'";
        } else if (dataBaseType == DataBaseType.Oracle) {
            sql = "select t.table_name from user_tables t";
        } else if (dataBaseType == DataBaseType.SQLServer) {
            sql = "select name from sys.tables";
        }
        try {
            rs = DBUtil.query(conn, sql, 50, 2);
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.closeDBResources(rs, conn);
        }
        return new ArrayList<>(tables);
    }


    public static List<String> getTableColumns(DataBaseType dataBaseType,
                                               String jdbcUrl, String user, String pass, String tableName) {
        Connection conn = getConnection(dataBaseType, jdbcUrl, user, pass, 2);
        return getTableColumnsByConn(dataBaseType, conn, tableName, "jdbcUrl:" + jdbcUrl);
    }

    public static List<String> getTableColumnsByConn(DataBaseType dataBaseType, Connection conn, String tableName, String basicMsg) {
        List<String> columns = new ArrayList<String>();
        Statement statement = null;
        ResultSet rs = null;
        String queryColumnSql = null;
        try {
            statement = conn.createStatement();
            queryColumnSql = String.format("select * from %s where 1=2",
                    tableName);
            rs = statement.executeQuery(queryColumnSql);
            ResultSetMetaData rsMetaData = rs.getMetaData();
            for (int i = 0, len = rsMetaData.getColumnCount(); i < len; i++) {
                columns.add(rsMetaData.getColumnName(i + 1));
            }

        } catch (SQLException e) {
            throw new RuntimeException("error");
        } finally {
            DBUtil.closeDBResources(rs, statement, conn);
        }

        return columns;
    }

    public static void closeDBResources(ResultSet rs,
                                        Connection conn) {
        DBUtil.closeDBResources(rs, null, conn);
    }

    public static void closeDBResources(ResultSet rs, Statement stmt,
                                        Connection conn) {
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException unused) {
            }
        }

        if (null != stmt) {
            try {
                stmt.close();
            } catch (SQLException unused) {
            }
        }

        if (null != conn) {
            try {
                conn.close();
            } catch (SQLException unused) {
            }
        }
    }
}
