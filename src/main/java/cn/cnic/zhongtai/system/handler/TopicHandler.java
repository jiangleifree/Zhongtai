package cn.cnic.zhongtai.system.handler;

import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.TaskService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Slf4j
@Component
public class TopicHandler implements TaskHandler {

    @Resource
    private TaskService taskService;

    @Override
    public void handle(Task nullTask) {


    }


}
