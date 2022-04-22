package cn.cnic.zhongtai.utils;


import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class RemoteShellExecutorHive {
    private Connection conn;
    /**
     * 远程机器IP
     */
    private String ip;

    /**
     * 用户名
     */
    private String osUsername;

    /**
     * 密码
     */
    private String password;
    private String charset = Charset.defaultCharset().toString();
    private static final int TIME_OUT = 1000 * 5 * 60;

    /**
     * 构造函数
     *
     * @param ip
     * @param usr
     * @param pasword
     */

    public RemoteShellExecutorHive(String ip, String usr, String pasword) {
        this.ip = ip;
        this.osUsername = usr;
        this.password = pasword;
    }


    /**
     * 登录
     *
     * @return
     * @throws IOException
     */
    private boolean login() throws IOException {
        conn = new Connection(ip);
        conn.connect();
        return conn.authenticateWithPassword(osUsername, password);
    }

    /**
     * 执行脚本
     *
     * @param cmds
     * @return
     * @throws Exception
     */
    public String exec(String cmds) throws Exception {
        InputStream stdOut = null;
        InputStream stdErr = null;
        String outStr = "";
        String outErr = "";
        int ret = -1;
        try {

            if (login()) {
                // Open a new {@link Session} on this connection
                Session session = conn.openSession();
                // Execute a command on the remote machine.

                session.execCommand(cmds);
                stdOut = new StreamGobbler(session.getStdout());

                outStr = processStream(stdOut, charset);
                stdErr = new StreamGobbler(session.getStderr());
                outErr = processStream(stdErr, charset);
                session.waitForCondition(ChannelCondition.EXIT_STATUS, TIME_OUT);
                System.out.println("outStr=" + outStr);
                System.out.println("outErr=" + outErr);

                ret = session.getExitStatus();

            } else {
                throw new Exception("登录远程机器失败" + ip); // 自定义异常类 实现略
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
            IOUtils.closeQuietly(stdOut);
            IOUtils.closeQuietly(stdErr);
        }
        return outErr;

    }

    /**
     * @param in
     * @param charset
     * @return
     * @throws IOException
     * @throws
     */

    private String processStream(InputStream in, String charset) throws Exception {

        byte[] buf = new byte[1024];

        StringBuilder sb = new StringBuilder();

        while (in.read(buf) != -1) {

            sb.append(new String(buf, charset));

        }

        return sb.toString();

    }

    // 创建JSONObject对象
    private static JSONObject createJSONObject() {
        JSONObject result = new JSONObject();
        result.put("table", "testw");//表名
        result.put("filePath", "/dataFiles/offlineFile/201911280/");//表名
        // 返回一个JSONArray对象
        JSONArray jsonArray = new JSONArray();
        JSONArray json = new JSONArray();
        json.add(0, "aaaa");
        json.add(1, "bbbb");
        json.add(2, "cccc");
        json.add(3, "dddd");
        json.add(4, "eeee");
        json.add(5, "ffff");
        result.put("columns",json);
        return result;
    }

    public static void main(String args[]) throws Exception {
        JSONObject createJSONObject = createJSONObject();
        System.out.println(createJSONObject.toString());
        String data = createJSONObject.toString().replace("\"", "\\\"");
        StringBuilder sparkCommand = new StringBuilder();
        System.out.println(data);
        sparkCommand
                 .append("export SPARK_MAJOR_VERSION=2;\n")
                .append("nohup spark-submit")
                .append(" --master yarn")
                .append(" --driver-memory 6g")
                .append(" --class com.network.All")
                .append(" --num-executors 3")
                .append(" --executor-memory 1g")
                .append(" --executor-cores 1")
                .append(" --jars /data/tobaccoZhongtai/spark/jar/json-20160810.jar,/data/tobaccoZhongtai/spark/jar/mysql-connector-java-5.1.47.jar /data/tobaccoZhongtai/spark/jar/hdfs_insert.jar")
                .append(" \"" + data + "\"\n")
                .append(" > /data/tobaccoZhongtai/spark/log/zhongtaiHdfs_insert.log 2>&1 &");

        System.out.println(sparkCommand.toString());
        RemoteShellExecutorHive executor = new RemoteShellExecutorHive("127.0.0.1","root","xxx");
    //     executor.exec("chmod 740 /data/tobaccoZhongtai/spark/jar/testByData.sh");
       // System.out.println(data);
        System.out.println(executor.exec(sparkCommand.toString()));
        // System.out.println(executor.exec("/data/tobaccoZhongtai/spark/jar/testByData.sh "+data));

    }

}
