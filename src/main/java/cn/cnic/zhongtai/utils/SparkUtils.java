package cn.cnic.zhongtai.utils;

import cn.cnic.zhongtai.system.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

@Slf4j
public class SparkUtils {

    public static String submitSparkTask(String sparkParamJson, String logFilePath, String jars) {
        StringBuilder sparkCommand = new StringBuilder();
        String sparkParamJson_repl = sparkParamJson.replace("\"", "\\\"");

        File logFile = new File(logFilePath);
        File parent = logFile.getParentFile();
        //先判断目录是否存在，不存在先创建
        if (!parent.exists()) {
            parent.mkdirs();
        }
        //创建日志文件
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sparkCommand.append("nohup spark-submit")
                .append(" --master yarn")
                .append(" --deploy-mode cluster")
                .append(" --driver-memory 6g")
                .append(" --class com.network.All")
                .append(" --num-executors 3")
                .append(" --executor-memory 1g")
                .append(" --executor-cores 1")
                .append(" --jars " + jars)
                .append(" \"" + sparkParamJson_repl + "\"")
                .append(" > " + logFilePath + " 2>&1 &");

        //submit指令
        UnixUtils.run(new String[]{"export SPARK_MAJOR_VERSION=2;" + sparkCommand.toString()}, false);

        //循环找appId 找不到就是循环100次
        String applicationId = searchAppId(logFile, "INFO YarnClientImpl: Submitted application");
        return applicationId;
    }

    private static String searchAppId(File logFile, String flag) {
        String applicationId = "";
        int count = 0;
        while (count < 100) {
            count++;
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (logFile.exists()) {
                BufferedReader reader = null;
                try {

                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        int appIdFlag = line.indexOf(flag);
                        if (appIdFlag != -1) {
                            String[] strings = line.split(" ");
                            applicationId = strings[strings.length - 1];
                            log.error("application id");
                            log.error(applicationId);
                            return applicationId;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return applicationId;

    }



    public static String  getSparkLogUrl(String applicationId,String url){
      //  String url = Constant.SPARK_LOG_URL + applicationId;
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(url + applicationId);
        String str = null;
        try {
            CloseableHttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            str = EntityUtils.toString(entity, "UTF-8");
        } catch (IOException e) {
            log.error("spark log info error :" +e);
            e.printStackTrace();
        }
        return str;

    }



    public static String getHtml(String urlStr) {

        // Define links to be accessed
        String url = urlStr;
        // Define a string to store web content
        String result = "";
        // Define a buffered character input stream
        BufferedReader in = null;
        try {
            // Convert string to url object
            URL realUrl = new URL(url);
            // Initialize a link to the "url" link
            URLConnection connection = realUrl.openConnection();
            // Start the actual connection
            connection.connect();
            // Initialize the "BufferedReader" input stream to read the response of the "URL"
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            // Used to temporarily store data for each fetched row
            String line;
            while ((line = in.readLine()) != null) {
                // Traverse each row that is fetched and store it in "result"
                result += line + "\n";
            }
        } catch (Exception e) {
            log.error("send get request is abnormal!" + e);
            e.printStackTrace();
        } // Use "finally" to close the input stream
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        log.info("html info:" + result);
        return result;
    }
}
