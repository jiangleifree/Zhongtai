package cn.cnic.zhongtai.system.handler;

import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.DataImportService;
import cn.cnic.zhongtai.system.service.TaskService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.List;

import static cn.cnic.zhongtai.system.Constant.INCLUDE_TASK;

@Component
public class UpdateDoingTaskHandler {

    @Resource
    private TaskService taskService;

    @Resource
    private DataImportService dataImportService;
    public void handle() {

        //获取所有waitingTask
        List<Task> doingTasks = taskService.getTasksByStatus(Task.DOING);
        for (int i = 0; i < doingTasks.size(); i++) {
            Task temp = doingTasks.get(i);
            if (!INCLUDE_TASK.contains(temp.getTaskType())){ //判断任务类型是否在可处理范围内
                return;
            }
            JSONObject param = JSON.parseObject(temp.getJsonData());
            String appId = param.getString("applicationId");
            if (getStatus(appId).equals("true")){
                //数据接入任务需要更新数据接入表状态
                //if("dataToHiveByHdfsTask".equals(temp.getTaskType())){
                if (Task.dataToHiveByHdfsTask.equals(temp.getTaskType())){
                    String id = param.getString("id");
                    dataImportService.changeDataImportProgress(Long.valueOf(id),Task.DONE);
                }
                taskService.changeTaskStatus(temp.getTaskId(), Task.DONE);
            }
        }
    }

    private String getStatus(String appId) {
        RestTemplate client = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        String jobsUrl = "http://127.0.0.1:18081/api/v1/applications/" + appId + "/jobs/";

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(headers);
        //  执行HTTP请求
        ResponseEntity<String> response = client.exchange(jobsUrl, HttpMethod.GET, requestEntity, String.class);
        if (response != null){

            JSONArray body = JSON.parseArray(response.getBody());
            for (int i = 0; i < body.size(); i ++){
                String status = body.getJSONObject(i).getString("status");
                if (!status.equals("SUCCEEDED")){
                    return "false";
                }
            }
        }
        return "true";
    }

}
