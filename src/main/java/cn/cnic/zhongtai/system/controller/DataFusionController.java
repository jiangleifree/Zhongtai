package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.Message;
import cn.cnic.zhongtai.system.service.DataFusionService;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/dataFusion")
public class DataFusionController {

    @Resource
    private DataFusionService dataFusionService;

    @PostMapping("/startFusion")
    public String startFusion(@RequestBody String jsonStr){
        Message ret;
        try {
            JSONObject data = JSONObject.parseObject(jsonStr);
            log.error(data.toJSONString());
            dataFusionService.startFusion(data);
            ret = new Message("加入融合任务");
        } catch (Exception e){
            ret = new Message(e);
        }
        return ret.toJsonNoException();
    }

}
