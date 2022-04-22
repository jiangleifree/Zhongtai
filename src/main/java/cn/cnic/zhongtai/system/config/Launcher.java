package cn.cnic.zhongtai.system.config;

import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.DataImportService;
import cn.cnic.zhongtai.system.service.TaskService;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkAppHandle;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

@Slf4j
@Component
@PropertySource({"classpath:config.properties"})
public class Launcher {

    @Value("${spark.home}")
    private String sparkHome;
    @Value("${spark.master}")
    private String sparkMaster;
    @Value("${spark.yarn.jars}")
    private String sparkYarnJars;
    @Value("${spark.deploy.mode}")
    private String sparkDeployMode;

    @Resource
    private TaskService taskService;

    @Resource
    private DataImportService dataImportService;

    public void CreateSparkLauncher(String appResource, String args, SparkAppHandle.Listener listener, File outputFile) throws IOException {
        CreateSparkLauncher(appResource, args, listener, "10g", "5", "8", "8g", "3", outputFile);
    }

    public void CreateSparkLauncher(String appResource, String args, SparkAppHandle.Listener listener,
                                    String driverM, String driverC,
                                    String executorNum, String executorM, String executorC, File outputFile) throws IOException {
        /*SparkAppHandle handle = new SparkLauncher()
                .redirectError()
                .redirectOutput(outputFile)
                .setAppName("MiddleStage")
                .setSparkHome(sparkHome)
                .setMaster(sparkMaster)
                .setConf("spark.yarn.jars", sparkYarnJars)
                .setConf("spark.driver.memory", driverM)
                .setConf("spark.driver.cores", driverC)
                .setConf("spark.executor.instances", executorNum)
                .setConf("spark.executor.memory", executorM)
                .setConf("spark.executor.cores", executorC)
                .setAppResource(appResource)
                //权限定名
                .setMainClass("com.network.All")
                .addAppArgs(args)
                .setDeployMode(sparkDeployMode)
                .startApplication(listener);*/
        log.error("spark 任务参数");
        log.error(args);

        SparkLauncher sparkLauncher = new SparkLauncher()
                .setAppName("MiddleStage")
                .setSparkHome(sparkHome)
                .setMaster(sparkMaster)
                .setConf("spark.yarn.jars", sparkYarnJars)
                .setConf("spark.driver.memory", driverM)
                .setConf("spark.driver.cores", driverC)
                .setConf("spark.executor.instances", executorNum)
                .setConf("spark.executor.memory", executorM)
                .setConf("spark.executor.cores", executorC)
                .setConf("spark.yarn.maxAppAttempts", "1")
                .setAppResource(appResource)
                //权限定名
                .setMainClass("com.network.All")
                .addAppArgs(args)
                .setDeployMode(sparkDeployMode);
        if (outputFile != null && outputFile.exists()) {
            sparkLauncher.redirectError()
                    .redirectOutput(outputFile);

        }
        sparkLauncher.startApplication(listener);

    }


    //不在使用，暂时保留
    public void sparkLaucher1(JSONObject param, String driverM, String driverC, String executorNum, String executorM, String executorC) {
        log.info("===============================进入sparkLaucher===============================");

        String jarPath = param.getString("jarPath");
        String paramStr = param.getString("param");
        String type = param.getString("type");

        String taskId = param.getString("taskId");

        driverM = null == driverM ? "2g" : driverM;
        driverC = driverC == null ? "1" : driverC;
        executorNum = executorNum == null ? "3" : executorNum;
        executorM = executorM == null ? "2g" : executorM;
        executorC = executorC == null ? "2" : executorC;
        SparkAppHandle handler = null;
        String status = "";
        try {
            handler = new SparkLauncher()
                    .setAppName(type)
                    .setSparkHome(sparkHome)
                    .setMaster(sparkMaster)
                    .setConf("spark.yarn.jars", sparkYarnJars)
                    .setConf("spark.driver.memory", driverM)
                    .setConf("spark.driver.cores", driverC)
                    .setConf("spark.executor.instances", executorNum)
                    .setConf("spark.executor.memory", executorM)
                    .setConf("spark.executor.cores", executorC)
                    .setConf("spark.hive.metastore.uris", "thrift://127.0.0.1:9083")
                    .setConf("spark.hive.metastore.warehouse.dir", "hdfs://127.0.0.1/apps/hive/warehouse")
                    .setAppResource(jarPath)
                    //权限定名
                    .setMainClass("com.network.All")
                    .addAppArgs(paramStr)
                    .setDeployMode(sparkDeployMode)
                    .startApplication(new SparkAppHandle.Listener() {
                        public void stateChanged(SparkAppHandle sparkAppHandle) {
                            log.info("===============================stateChanged============" + sparkAppHandle.getAppId() + "===================");
                            if (sparkAppHandle.getState().isFinal()) {
                                log.info("===============================程序终止状态===============================");

                                taskService.changeTaskStatus(Integer.parseInt(taskId), sparkAppHandle.getState().toString());
                                log.info("===============================最终状态更新完成===============================");

                                if (Task.dataToHiveByHdfsTask.equals(type)) {
                                    dataImportService.changeDataImportProgress(Long.valueOf(param.getString("id")), sparkAppHandle.getState().toString());
                                    log.info("===============================最终接入状态更新完成===============================");

                                }
                            }

                        }

                        public void infoChanged(SparkAppHandle sparkAppHandle) {
                            log.info("===============================infoChanged=============" + sparkAppHandle.getAppId() + "==================");

                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
            log.error("sparkLaucher1 执行错误" + e);
        }
        int x = 0;
        while (!"FINISHED".equalsIgnoreCase(handler.getState().toString()) && !"FAILED".equalsIgnoreCase(handler.getState().toString())) {
            log.info("===============================循环中===============================");
            log.info("===========" + handler.getAppId() + "====================" + status + "============" + handler.getState().toString() + "===================");
            if (null != handler.getAppId() && !"null".equals(handler.getAppId()) && x == 0) {
                x++;
                taskService.changeTaskAppId(Integer.parseInt(taskId), handler.getAppId());
                log.info("===============================成功更新appid " + handler.getAppId() + "===============================");
            }
            if (!status.equals(handler.getState().toString())) {
                status = handler.getState().toString();
                taskService.changeTaskStatus(Integer.parseInt(taskId), status);
                log.info("===============================不同状态更新" + status + "===============================");
                if (Task.dataToHiveByHdfsTask.equals(type)) {
                    dataImportService.changeDataImportProgress(Long.valueOf(param.getString("id")), status);
                    log.info("===============================数据接入不同状态更新" + status + "===============================");
                }
            }
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error("while 循环 执行错误" + e);
            }
        }
    }


}