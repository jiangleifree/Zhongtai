package cn.cnic.zhongtai.system.handler;

import cn.cnic.zhongtai.system.mapper.MapStorageMapper;
import cn.cnic.zhongtai.system.model.MapPort;
import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.MapPortService;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.CommonUtils;
import cn.cnic.zhongtai.utils.HdfsUtil;
import cn.cnic.zhongtai.utils.UnixUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.Arrays;

@Component
@Slf4j
public class MapTaskHandler {

    @Resource
    private TaskService taskService;
    @Resource
    private MapStorageMapper mapStorageMapper;
    @Resource
    private MapPortService mapPortService;
    @Resource
    private HdfsUtil hdfsUtil;

    public void handle() {
        //查询任务为Neo4jTask且status为done
        Task oneTask = taskService.getOneMapTask();
        if (oneTask == null) {
            return;
        }
        JSONObject param = JSON.parseObject(oneTask.getJsonData());
        String nodes = param.getString("nodes");
        String relationShips = param.getString("relationShips");
        String mapNameEn = param.getString("mapNameEn");
        String fileCount = param.getString("fileCount");
        String mapPath = param.getString("mapPath");

        //从hdfs get 原始库文件
        String localFilePath = "/data/tobaccoZhongtai/neo4j/neo4j_data" + mapPath;
        String hdfsFilePath = "/zhongtai/neo4j_data/" + mapPath;
        hdfsUtil.get(localFilePath, hdfsFilePath);

        String newInstanceDir = "/data/tobaccoZhongtai/neo4j/neo4j-3.5.8/" + mapPath;
        //cp一个新的图库实例
        UnixUtils.run(new String[]{"cp -r /data/tobaccoZhongtai/neo4j/neo4j-3.5.8/neo4j-original/ " + newInstanceDir}, false);

        //获取已经使用的最大端口号
        Integer maxPort = mapPortService.getMaxPort();
        int boltPort;
        int httpPort;
        int httpsPort;

        if (maxPort == null) {
            boltPort = 7474;
            httpPort = 7475;
            httpsPort = 7476;
        } else {
            boltPort = maxPort + 1;
            httpPort = maxPort + 2;
            httpsPort = maxPort + 3;
        }

        //记录端口号
        MapPort mapPort = new MapPort();
        mapPort.setBoltPort(boltPort);
        mapPort.setHttpPort(httpPort);
        mapPort.setHttpsPort(httpsPort);
        mapPort.setMapName(mapNameEn);
        mapPortService.create(mapPort);

        //往配置文件追加端口号
        StringBuilder echo = new StringBuilder();
        echo.append("echo -e 'dbms.connector.bolt.listen_address=:")
                .append(boltPort)
                .append("\n dbms.connector.http.listen_address=:")
                .append(httpPort)
                .append("\n dbms.connector.https.listen_address=:")
                .append(httpsPort)
                .append("' >> /data/tobaccoZhongtai/neo4j/neo4j-3.5.8/")
                .append(mapPath)
                .append("/conf/neo4j.conf");
        UnixUtils.run(new String[]{echo.toString()}, false);

        //创建日志文件
        String logPath = "/data/tobaccoZhongtai/neo4j/log/daoru" + CommonUtils.getUUID32() + ".log";
        File logFile = new File(logPath);
        if (!logFile.exists()){
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String neo4jCommand = generateImportCommand(mapNameEn, nodes, relationShips, mapPath, logPath);
        //导入图库命令执行
        UnixUtils.run(new String[]{neo4jCommand}, false);
        //阻塞判断任务是否完成
        getStatus(logFile, "IMPORT DONE in");

        //启动图库  docker
        String dockerCommand = generateDockerCommand(mapNameEn, httpPort, boltPort);
        UnixUtils.run(new String[]{dockerCommand}, false);

        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //启动状态设置为doing
        mapStorageMapper.changeMapToDoingByMapNameEn(mapNameEn);
        //设置任务状态为donedone
        taskService.changeTaskStatus(oneTask.getTaskId(), "donedone");
    }

    private String generateDockerCommand2(String mapNameEn, int httpPort, int boltPort){
        //启动图库  docker
        StringBuilder dockerCommand = new StringBuilder();
        dockerCommand.append("docker run -d --name ")
                .append(mapNameEn)
                .append(" -v ")
                .append("/data/tobaccoZhongtai/neo4j/neo4j-3.5.8/")
                .append(mapNameEn)
                .append("/data:/data -p ")
                .append(httpPort)
                .append(":")
                .append(7474)
                .append(" -p ")
                .append(boltPort)
                .append(":")
                .append(7687)
                .append(" neo4j-origal");
        return dockerCommand.toString();
    }

    //生成docker命令
    private String generateDockerCommand(String mapNameEn, int httpPort, int boltPort){
        //启动图库  docker
        StringBuilder dockerCommand = new StringBuilder();
        dockerCommand.append("docker run -d --name ")
                .append(mapNameEn)
                .append(" -v ")
                .append("/data/tobaccoZhongtai/neo4j/neo4j-3.5.8/")
                .append(mapNameEn)
                .append("/data:/data -v /data/tobaccoZhongtai/neo4j/neo4j-3.5.8/")
                .append(mapNameEn)
                .append("/conf:/conf -p ")
                .append(httpPort)
                .append(":")
                .append(httpPort)
                .append(" -p ")
                .append(boltPort)
                .append(":")
                .append(boltPort)
                .append(" neo4j-docker");
        return dockerCommand.toString();
    }

    private String getStatus(File logFile, String flag) {
        int count = 0;
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

                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        int appIdFlag = line.indexOf(flag);
                        if (appIdFlag != -1) {
                            return "true";
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
        return "false";
    }

    private String generateImportCommand(String mapName, String nodes, String relationShips, final String mapPath, String logPath) {
        StringBuilder neo4jCommand = new StringBuilder();
        neo4jCommand.append("nohup")
                .append(" /data/tobaccoZhongtai/neo4j/neo4j-3.5.8/" + mapPath + "/bin/neo4j-admin import --id-type=STRING")
                .append(" --delimiter=☔")
                .append(" --ignore-extra-columns=true")
                .append(" --ignore-duplicate-nodes=true")
                .append(" --ignore-missing-nodes=true")
                .append(" --database=graph.db");

        Arrays.stream(nodes.split(",")).forEach(node -> {
            neo4jCommand.append(" --nodes:")
                    .append(node)
                    .append("=\"/data/tobaccoZhongtai/neo4j/neo4j_data/" + mapPath + "/")
                    .append(node)
                    .append("head/part-00000,/data/tobaccoZhongtai/neo4j/neo4j_data/" + mapPath + "/")
                    .append(node)
                    .append("/part-00000\"");
        });
        Arrays.stream(relationShips.split(",")).forEach(relationship -> {
            neo4jCommand.append(" --relationships:")
                    .append(relationship)
                    .append("=\"/data/tobaccoZhongtai/neo4j/neo4j_data/" + mapPath + "/")
                    .append(relationship)
                    .append("head/part-00000,/data/tobaccoZhongtai/neo4j/neo4j_data/" + mapPath + "/")
                    .append(relationship)
                    .append("/part-00000\"");
        });
        neo4jCommand.append(" > " + logPath + " 2>&1 &");
        return neo4jCommand.toString();
    }
}
