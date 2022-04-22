package cn.cnic.zhongtai.system.listener;

import cn.cnic.zhongtai.system.model.MapPort;
import cn.cnic.zhongtai.system.model.MapStorage;
import cn.cnic.zhongtai.system.neo4j.Neo4jFactory;
import cn.cnic.zhongtai.system.service.MapPortService;
import cn.cnic.zhongtai.system.service.MapStorageService;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.*;
import com.google.common.io.Files;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.launcher.SparkAppHandle;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cn.cnic.zhongtai.system.Constant.NEO4J_ORIGINAL_LOCATION;

@Slf4j
public class GenerateNeo4jMetaDataListener implements SparkAppHandle.Listener {

    private String stdputLogPath;
    private String errputLogPath;
    private Map<String, Object> sparkParam;
    private String neo4jRepositoryName;
    private int taskId;
    private TaskService taskService;
    private MapStorageService mapStorageService;
    private MapPortService mapPortService;
    private HdfsUtil hdfsUtil;
    private static final String NEO4J_HOME = "/data/tobaccoZhongtai/neo4j/";
    private static final String NEO4J_HOME_DOCKER = NEO4J_HOME + "neo4j-3.5.8/map-docker/";
    private static final String NEO4J_CSV_HOME = "/data/tobaccoZhongtai/neo4j/csv/";
    private static final String HDFS_NEO4J_CSV_PATH = "/zhongtai/neo4j_data/";

    public GenerateNeo4jMetaDataListener() {
    }

    public GenerateNeo4jMetaDataListener(int taskId, String neo4jRepositoryName, Map<String, Object> sparkParam) {
        this.taskId = taskId;
        this.neo4jRepositoryName = neo4jRepositoryName;
        this.sparkParam = sparkParam;
        this.taskService = BaseHolder.getBean("taskServiceImpl");
        this.mapStorageService = BaseHolder.getBean("mapStorageServiceImpl");
        this.mapPortService = BaseHolder.getBean("mapPortServiceImpl");
        this.hdfsUtil = BaseHolder.getBean("hdfsUtil");
    }

    @Override
    public void stateChanged(SparkAppHandle handle) {
        if (handle.getState().equals(SparkAppHandle.State.FAILED)) {
            taskService.changeTaskStatus(Integer.valueOf(taskId), "生成图库元数据失败");
        } else if (handle.getState().equals(SparkAppHandle.State.FINISHED)) {
            handle();
        } else if (handle.getState().equals(SparkAppHandle.State.SUBMITTED)) {
            if (null != handle.getAppId() && !"null".equals(handle.getAppId())) {
                taskService.changeTaskAppId(taskId, handle.getAppId());
                log.info("================" + handle.getState() + "状态===============成功更新appid " + handle.getAppId() + "===============================");
            }
        } else if (handle.getState().equals(SparkAppHandle.State.RUNNING)) {
            taskService.changeTaskStatus(Integer.valueOf(taskId), "正在生成图库元数据");
        }
    }

    @Override
    public void infoChanged(SparkAppHandle handle) {

    }

    public void handle() {

        taskService.changeTaskStatus(Integer.valueOf(taskId), "正在创建图库");

        String nodes = "";
        String relationShips = "";
        String mapNameEn = "";
        String mapPath = "";
        try {
            nodes = (String) sparkParam.get("nodes");
            relationShips = (String) sparkParam.get("relationShips");
            mapNameEn = (String) sparkParam.get("mapNameEn");
            mapPath = (String) sparkParam.get("mapPath");
        } catch (Exception e) {
            log.error(e.getMessage());
            taskService.changeTaskStatus(taskId, "获取json参数失败");
            return;
        }

        //从hdfs get csv文件
        String localCsvFilePath = NEO4J_CSV_HOME + mapPath;
        boolean var = new File(localCsvFilePath).mkdirs();
        if (!var) {
            taskService.changeTaskStatus(taskId, "csv文件夹已经存在");
            return;
        }
        String hdfsFilePath = HDFS_NEO4J_CSV_PATH + mapPath;
        try {
            RetryUtil.executeWithRetry(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    hdfsUtil.copyHdfsDirToLocalDir(hdfsFilePath, localCsvFilePath);
                    return null;
                }
            }, 10, 1000L, false);
        } catch (IllegalArgumentException e) {

        } catch (Exception e) {
            e.printStackTrace();
            taskService.changeTaskStatus(taskId, "从hdfs获取csv文件失败");
            return;
        }

        String newInstanceDir = NEO4J_HOME_DOCKER + mapPath;
        //cp一个新的图库实例
        try {
            FileUtils.copyDirectory(new File(NEO4J_ORIGINAL_LOCATION), new File(newInstanceDir));
            cn.cnic.zhongtai.utils.FileUtils.changeFolderPermission(new File(newInstanceDir + "/bin/neo4j-admin"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //获取已经使用的最大端口号
        Integer maxPort = mapPortService.getMaxPort();
        int boltPort = 7474;
        int httpPort = 7475;
        int httpsPort = 7476;
        if (maxPort != null) {
            boltPort = maxPort + 1;
            httpPort = boltPort + 1;
            httpsPort = httpPort + 1;
        }
        //查询端口是否已经被占用 如果占用返回未占用的端口
        int[] ports = checkPort(boltPort, httpPort, httpsPort);
        boltPort = ports[0];
        httpPort = ports[1];
        httpsPort = ports[2];

        //记录端口号
        MapPort mapPort = new MapPort();
        mapPort.setBoltPort(boltPort);
        mapPort.setHttpPort(httpPort);
        mapPort.setHttpsPort(httpsPort);
        mapPort.setMapName(mapNameEn);
        mapPortService.create(mapPort);

        //往配置文件追加端口号
        StringBuilder content = new StringBuilder();

        content.append("\n dbms.connector.bolt.listen_address=:")
                .append(boltPort)
                .append("\n dbms.connector.http.listen_address=:")
                .append(httpPort)
                .append("\n");
                //.append("\ndbms.connector.https.listen_address=:")
                //.append(httpsPort);

        String targetConfigPath = newInstanceDir + "/conf/neo4j.conf";
        //UnixUtils.run(new String[]{content.toString()}, false);
        cn.cnic.zhongtai.utils.FileUtils.appendContentToFile(targetConfigPath, content.toString());

        //neo4j-admin import 日志文件
        String logPath = "/data/tobaccoZhongtai/neo4j/log/daoru" + CommonUtils.getUUID32() + ".log";
        String neo4jCommand = generateImportCommand(nodes, relationShips, logPath, localCsvFilePath, newInstanceDir);
        //neo4j-admin import 命令执行
        UnixUtils.run(new String[]{neo4jCommand}, false);

        //阻塞判断任务是否完成  判断import是否完成
        try {
            if (!getStatus(new File(logPath), "IMPORT DONE in")) { //import失败
                taskService.changeTaskStatus(taskId, "import data failed, " + logPath);
                return;
            }
        } catch (Exception e) {
            taskService.changeTaskStatus(taskId, "import data failed, " + logPath);
            return;
        }

        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //启动图库  docker
        String dockerCommand = generateDockerCommand(newInstanceDir, mapNameEn, httpPort, boltPort);
        UnixUtils.run(new String[]{dockerCommand}, false);

        boolean isOpen = false;
        try {
            int count = 0;
            while(true) {
                isOpen = checkPortIsOpen(boltPort);
                if (isOpen) {
                    String url = "bolt://127.0.0.1" + ":" + boltPort;
                    Neo4jFactory.newDriver(url, "", "").ifPresent(driver -> Neo4jFactory.addDriverToPool(url, driver));
                    break;
                } else {
                    Thread.sleep(2000);
                }
                count++;
                if (count > 10) {
                    break;
                }

            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (isOpen) {
            //启动状态设置为doing
            mapStorageService.changeStatusByMapNameEn(mapNameEn, MapStorage.DOING);
        } else {
            //启动状态设置为closed
            mapStorageService.changeStatusByMapNameEn(mapNameEn, MapStorage.CLOSED);
        }
        //设置任务状态为done
        taskService.changeTaskStatus(Integer.valueOf(taskId), "图库创建完成");


    }

    /**
     * 检查端口是否占用, 如果占用, 返回没有占用的端口号
     *
     * @param boltPort
     * @param httpPort
     * @param httpsPort
     * @return
     */
    private int[] checkPort(int boltPort, int httpPort, int httpsPort) {
        int boltPortRet = boltPort;
        int httpPortRet = httpPort;
        int httpsPortRet = httpsPort;
        while (true) {
            Socket socket = null;
            try {
                socket = new Socket("127.0.0.1", boltPortRet);
                boltPortRet = boltPortRet + 1;
            } catch (Exception e) {
                break;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        httpPortRet = boltPortRet + 1;
        while (true) {
            Socket socket = null;
            try {
                socket = new Socket("127.0.0.1", httpPortRet);
                httpPortRet = httpPortRet + 1;
            } catch (Exception e) {
                break;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        httpsPortRet = httpPortRet + 1;
        while (true) {
            Socket socket = null;
            try {
                socket = new Socket("127.0.0.1", httpsPortRet);
                httpsPortRet = httpsPortRet + 1;
            } catch (Exception e) {
                break;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return new int[]{boltPortRet, httpPortRet, httpsPortRet};
    }

    private boolean checkPortIsOpen(int port) {
        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", port);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //生成docker命令
    private String generateDockerCommand(String neo4jHome,
            String mapNameEn, int httpPort, int boltPort) {
        //启动图库  docker
        StringBuilder dockerCommand = new StringBuilder();
        dockerCommand.append("docker run -d --name ")
                .append(mapNameEn)
                .append(" -v ")
                .append(neo4jHome)
                .append("/data:/data -v ")
                .append(neo4jHome)
                .append("/conf:/conf -p ")
                .append(httpPort)
                .append(":")
                .append(httpPort)
                .append(" -p ")
                .append(boltPort)
                .append(":")
                .append(boltPort)
                .append(" --env NEO4J_AUTH=none ")
                .append(" neo4j-iframe:3.5.8");
        return dockerCommand.toString();
    }

    @Data
    private static class LogTailerListener implements TailerListener {
        private volatile boolean status = false;
        private String flag;
        private Observable observable;

        public LogTailerListener() {
        }

        public LogTailerListener(String flag, Observer observer) {
            this.flag = flag;
            this.observable = new Observable();
            observable.addObserver(observer);
        }

        public boolean getStatus() {
            return status;
        }

        @Override
        public void init(Tailer tailer) {

        }

        @Override
        public void fileNotFound() {  //文件没有找到
            log.error("文件没有找到");
        }

        @Override
        public void fileRotated() {  //文件被外部的输入流改变
            log.error("文件rotated");
        }

        @Override
        public void handle(String line) { //增加的文件的内容
            log.error("文件line:" + line);
            if (StringUtils.isNotBlank(line)
                    && line.contains(flag)) {
                status = true;
                observable.notifyObservers();
                //Thread.currentThread().stop();
            }

            if (StringUtils.isNotBlank(line)
                    && line.contains("unexpected error")) {
                observable.notifyObservers();
                //Thread.currentThread().stop();
            }
        }


        @Override
        public void handle(Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class MyObserver implements Observer{

        @Override
        public void update(Observable o, Object arg) {

        }
    }

    //通过查日志的方式判断 neo4j-admin import 命令是否执行成功
    private boolean getStatus2(String logPath, String flag) throws IOException, InterruptedException {

        File logFile = new File(logPath);

        //FileUtils.touch(logFile);
        Observer observer = new MyObserver();
        LogTailerListener logTailerListener = new LogTailerListener(flag, observer);
        Tailer tailer = new Tailer(logFile, logTailerListener, 4000, true);

        ExecutorService es = Executors.newFixedThreadPool(1);
        es.submit(tailer);
        es.shutdown();
        es.awaitTermination(1, TimeUnit.MINUTES);

        return logTailerListener.getStatus();
    }


    /**
     * 循环遍历日志文件
     * 1分钟文件没有输出(文件大小没有变化), return false
     * @param logFile
     * @param flag
     * @return
     */
    private boolean getStatus(File logFile, String flag) {
        int count = 0;
        long fileSize = 0;
        long lastFileSize = 0;
        while (count < 500) {
            count++;
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (logFile.exists()) {
                BufferedReader reader = null;
                try {
                    long currentFileSize = FileUtils.sizeOf(logFile);
                    //和循环前面12次进行对比, 文件大小没有变化, 说明1分钟文件没有输出
                    //1分钟文件没有输出, 退出
                    if ((count % 12) == 0) {
                        if (fileSize < currentFileSize) {
                            fileSize = currentFileSize;
                        } else {
                            return false;
                        }
                    }

                    if (lastFileSize == currentFileSize) { //如果文件大小没有变化, 不进行读取
                        continue;
                    }
                    lastFileSize = currentFileSize;
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        int appIdFlag = line.indexOf(flag);
                        if (appIdFlag != -1) {
                            return true;
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
        return false;
    }

    private String generateImportCommand(String nodes,
                                         String relationShips,
                                         String logPath,
                                         String csvPath,
                                         String neo4jHome) {
        StringBuilder neo4jCommand = new StringBuilder();
        neo4jCommand.append("nohup ")
                //.append(" /data/tobaccoZhongtai/neo4j/neo4j-3.5.8/neo4j-docker/" + mapPath + "/bin/neo4j-admin import --id-type=STRING")
                .append(neo4jHome + "/bin/neo4j-admin import --id-type=STRING")
                .append(" --delimiter=☔")
                .append(" --ignore-extra-columns=true")
                .append(" --ignore-duplicate-nodes=true")
                .append(" --ignore-missing-nodes=true")
                .append(" --database=graph.db");

        Arrays.stream(nodes.split(",")).forEach(node -> {
            neo4jCommand.append(" --nodes:")
                    .append(node)
                    //.append("=\"/data/tobaccoZhongtai/neo4j/neo4j_data/" + mapPath + "/")
                    .append("=\"" + csvPath + "/")
                    .append(node + "head/part-00000,")
                    .append(csvPath + "/")
                    .append(node)
                    .append("/part-00000\"");
        });
        Arrays.stream(relationShips.split(",")).forEach(relationship -> {
            neo4jCommand.append(" --relationships:")
                    .append(relationship)
                    //.append("=\"/data/tobaccoZhongtai/neo4j/neo4j_data/" + mapPath + "/")
                    .append("=\"" + csvPath + "/")
                    .append(relationship)
                    //.append("head/part-00000,/data/tobaccoZhongtai/neo4j/neo4j_data/" + mapPath + "/")
                    .append("head/part-00000,")
                    .append(csvPath + "/")
                    .append(relationship)
                    .append("/part-00000\"");
        });
        neo4jCommand.append(" > " + logPath + " 2>&1 &");
        return neo4jCommand.toString();
    }
}
