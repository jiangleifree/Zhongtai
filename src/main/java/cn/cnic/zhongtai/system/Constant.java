package cn.cnic.zhongtai.system;


import java.util.ArrayList;
import java.util.List;

public class Constant {
    public static final int SOCKET_TIMEOUT_INSECOND = 172800;
    public static final int TIMEOUT_SECONDS = 15;
    public static final String NEO4J_URI_7472 = "bolt://127.0.0.1:7472";
    public static final String NEO4J_PRE = "yancao";
    public static final String DEFAULT_HDFS = "hdfs://127.0.0.1:8020";
    public static final String HDFS_PATH_PRE = "/dataFiles/";
    public static final String HDFS_PATH_PRE_OFFLINE = "/dataFiles/offline/";
    public static final String HDFS_DELIMITER = "☔";
    public static final String SPARK_IMPORT_DATA_TASK_LOG_PATH = "/data/tobaccoZhongtai/spark/log/spark_import_data_task";
    public static final String SPARK_DATA_PROCESS_TASK_LOG_PATH = "/data/tobaccoZhongtai/spark/log/data_process_task";
    public static final String DATAX_JOB_PATH = "/data/tobaccoZhongtai/datax/datax/job/";
    //public static final String SPARK_JAR_NEO4J = "/data/tobaccoZhongtai/spark/jar/json-20160810.jar /data/tobaccoZhongtai/spark/jar/neo4j.jar";
    public static final String SPARK_JAR_NEO4J = "/data/tobaccoZhongtai/spark/jar/neo4j.jar";
    public static final String MYSQL_URL = "jdbc:mysql://127.0.0.1:3307/dp2?useUnicode=true&characterEncoding=utf8&useSSL=false&allowMultiQueries=true";
    public static final String SPARK_JAR_TOPIC = "/data/tobaccoZhongtai/spark/jar/mysql-connector-java-5.1.47.jar /data/tobaccoZhongtai/spark/jar/zhongtai.jar";
    public static final String SPARK_JAR_INPUT_DATA = "/data/tobaccoZhongtai/spark/jar/dataToHive_new.jar";
    public static final String SPARK_JAR_ORC_TO_HIVE = "/data/tobaccoZhongtai/spark/jar/orcToHive.jar";
    public static final String SPARK_JAR_FUSION = "/data/tobaccoZhongtai/spark/jar/fusion.jar";
    public static final String SPARK_JAR_CLEAR = "/data/tobaccoZhongtai/spark/jar/transform.jar";
    public static final char HDFS_DELIMITER_CHAR = '☔';
    public static final String ONLINE = "online";
    public static final String OFFLINE = "offline";
    public static final String FILE_FORMAT_TEXT = "text";
    public static final String SPARK_LOG_URL = "http://127.0.0.1:8088/ws/v1/cluster/apps/";
    public static final String PARTICIPLEE_FILE = "/data/bacco_dict.txt";
    public static final String PARTICIPLE_JAR = "/data/tobaccoZhongtai/spark/jar/participle.jar";
    public static final String RELATION_CREATE_JAR = "/data/tobaccoZhongtai/spark/jar/zhongjianbiao.jar";
    public static final String NEO4J_ORIGINAL_LOCATION = "/data/tobaccoZhongtai/neo4j/neo4j-3.5.8/neo4j-original/";
    public static final String RELATION_NEO4J_JAR = "/data/tobaccoZhongtai/spark/jar/relation_neo4j.jar";
    public static final String RELATION_EXTRACT = "/data/tobaccoZhongtai/spark/jar/relation.jar";
    public static final String TO_MYSQL_JAR = "/data/tobaccoZhongtai/spark/jar/toMysql.jar";
    public static final String DATA_STATISTICAL_INFORMATION_JAR = "/data/tobaccoZhongtai/spark/jar/depTable.jar";


    //请求方式
    public static final String POST = "Post";
    public static final String GET = "Get";
    public static final String DELETE = "Delete";
    public static final String PUT = "Put";
    public static final String TRACE = "Trace";
    public static final String HEAD = "Head";
    public static final String OPTIONS = "Options";



    public static final List<String> INCLUDE_TASK = new ArrayList<>();

    static {

        //INCLUDE_TASK.add(Task.dataToHiveByHdfsTask);
        //INCLUDE_TASK.add(Task.Neo4jTask);
        //主题库使用的回调
        //INCLUDE_TASK.add(Task.TopicTask);
        //INCLUDE_TASK.add(Task.DATA_CLEAR_TASK);

    }

    public static final String OB10_SPLIT_STRING = "||_dsc_ob10_dsc_||";
    public static final String OB10_SPLIT_STRING_PATTERN = "\\|\\|_dsc_ob10_dsc_\\|\\|";

    public static class TopicConstant {
        public static final String TOPIC_URL_PRE = "http://127.0.0.1:666/api/topicService/";
    }
}
