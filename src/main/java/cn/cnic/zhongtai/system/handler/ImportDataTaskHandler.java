package cn.cnic.zhongtai.system.handler;

import cn.cnic.zhongtai.system.dao.HiveDao;
import cn.cnic.zhongtai.system.dao.SqlDao;
import cn.cnic.zhongtai.system.model.ImportDataTask;
import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.LogInfoService;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.SFTPUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@Component
@Slf4j
public class ImportDataTaskHandler implements TaskHandler {

    @Resource
    private TaskService taskService;
    @Resource
    private HiveDao hiveDao;
    @Resource
    private SqlDao sqlDao;
    @Resource
    private LogInfoService logInfoService;
    public static final SFTPUtil MYSQL_FTP = new SFTPUtil("root", "xxx", "127.0.0.1", 22);
    public static final String SQL_IMPORT_PATH_PRE = "/var/lib/mysql-files/tobaccoZhongtai/import/";
    public static final String SQL_IMPORT_PATH = "var/lib/mysql-files/tobaccoZhongtai/import";

    @Override
    public void handle(Task oneTask) {
        ImportDataTask currentTask = (ImportDataTask) oneTask;
        FileInputStream fis = null;
        File csvFile = null;
        try {

            //修改任务状态为doing
            taskService.changeTaskStatus(currentTask.getTaskId(), ImportDataTask.DOING);
            //添加log
            logInfoService.insertLogInfo(currentTask, "开始执行");

            String jsonData = oneTask.getJsonData();
            JSONObject data = JSONObject.parseObject(jsonData);
            String tableName = data.getString("tableName");
            String columns = data.getString("columns");
            String topicName = data.getString("topicName");

            //往tableInfo中插入数据
            //tableInfoMapper.createTableInfo(temp);

            //往创建的table中copy数据

            //hive数据导出到csv文件
            logInfoService.insertLogInfo(currentTask, "hive导出数据到csv文件开始");
            String filePath = hiveDao.exportHiveDataToCsv(tableName, columns);
            logInfoService.insertLogInfo(currentTask, "hive导出数据到csv文件结束");

            //把csv文件sftp到mysql节点
            logInfoService.insertLogInfo(currentTask, "开始上传csv文件到mysql节点");
            String dbNameAndTableName = topicName + "." + tableName;
            MYSQL_FTP.login();
            csvFile = new File(filePath);
            boolean isUpload = false;
            if (csvFile.exists()) { //如果hive导出的csv文件存在
                fis = new FileInputStream(csvFile);
                String fileName = csvFile.getName();
                isUpload = MYSQL_FTP.upload("/", SQL_IMPORT_PATH, fileName, fis);
                if (isUpload) { //csv文件上传sql节点成功
                    //load数据到mysql
                    logInfoService.insertLogInfo(currentTask, "上传csv文件到mysql节点成功");

                    logInfoService.insertLogInfo(currentTask, "mysql加载csv文件开始");
                    sqlDao.importDataFromCsv(SQL_IMPORT_PATH_PRE + fileName, dbNameAndTableName);
                    logInfoService.insertLogInfo(currentTask, "mysql加载csv文件结束");

                    if (currentTask.getInterval() == 0) {  //任务执行一次
                        logInfoService.insertLogInfo(currentTask, "任务执行完成");
                        taskService.changeTaskStatus(currentTask.getTaskId(), ImportDataTask.DONE);
                    } else {  //周期任务
                        logInfoService.insertLogInfo(currentTask, "任务执行完成");
                        taskService.changeTaskStatus(currentTask.getTaskId(), ImportDataTask.WAITING);
                        Calendar calendar = new GregorianCalendar();
                        calendar.setTime(new Date());
                        calendar.add(Calendar.DATE, (int) currentTask.getInterval());
                        taskService.updateNextTime(currentTask.getTaskId(), calendar.getTime());
                    }
                } else {  //csv文件上传sql节点失败
                    log.error("upload failed");
                    logInfoService.insertLogInfo(currentTask, "上传csv文件到mysql节点失败");
                }
            } else {  //hive导出的csv文件不存在
                logInfoService.insertLogInfo(currentTask, "hive导出的csv文件不存在");
                taskService.changeTaskStatus(currentTask.getTaskId(), ImportDataTask.FAILED);
            }

        } catch (Exception e) {
            e.printStackTrace();
            logInfoService.insertLogInfo(currentTask, "任务执行失败：" + e.getMessage());
            taskService.changeTaskStatus(currentTask.getTaskId(), ImportDataTask.FAILED);
        } finally {
            if (fis != null) {
                try {
                    fis.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (csvFile != null) {
                //删除临时csv文件
                csvFile.delete();
            }
        }
    }
}
