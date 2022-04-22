package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.Message;
import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.utils.DateUtils;
import cn.cnic.zhongtai.utils.ParamConvert;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.junit.Test;
import org.springframework.http.HttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/get1")
    public Message get1(String id) {
        return new Message();
    }

    @GetMapping("/get2")
    public Map<String, Object> get2(String id) {
        Map<String, Object> ret = new HashMap<>();
        ret.put("id", id);
        ret.put("test", "test");
        return ret;
    }

    @GetMapping("/http/import")
    public Map<String, Object> test111() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("code", 200);

        Map<String, Object> data1 = new HashMap<>();
        data1.put("name", "jianglei");
        data1.put("age", 111);
        data1.put("sex", "man");
        List<Map<String, Object>> datas = new ArrayList(){{add(data1);}};

        ret.put("data", datas);
        return ret;
    }

    @GetMapping("/testDelay")
    public void testDelay() {
        try {
            System.out.println("start");
            Thread.sleep(5000);
            System.out.println("end");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/post1")
    public Message post1(String id) {
        return new Message();
    }

    @PostMapping("/post2")
    @ApiOperation(value = "post2", response = Map.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "id", required = true, dataType = "String")
    })
    public Map<String, Object> post2(String id) {
        Map<String, Object> ret = new HashMap<>();
        ret.put("id", id);
        ret.put("test", "test");
        return ret;
    }

    @PostMapping("/testDate")
    public String testDate(Date date, HttpServletRequest request) {


        try {

            request.getParameterMap();

            request.getParameter("date");


        } catch (Exception e) {
            e.printStackTrace();
        }
        return "TestDate";

    }

    @PostMapping("/testReturnList")
    public List<Task> testReturnList() {
        List<Task> taskList = new ArrayList<>();
        Task task = new Task();
        taskList.add(task);
        return taskList;
    }


}
