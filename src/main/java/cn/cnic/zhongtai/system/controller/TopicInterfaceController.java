package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.Message;
import cn.cnic.zhongtai.system.model.TopicInterfaceStatistics;
import cn.cnic.zhongtai.system.service.InterfaceParamService;
import cn.cnic.zhongtai.system.service.TopicInterfaceService;
import cn.cnic.zhongtai.system.service.TopicInterfaceStatisticsService;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static cn.cnic.zhongtai.system.Constant.TopicConstant.TOPIC_URL_PRE;
import static cn.cnic.zhongtai.utils.IpAddressUtil.getUserIP;

@RestController
@RequestMapping("/api/topicService")
public class TopicInterfaceController {

    @Resource
    private TopicInterfaceService topicInterfaceService;
    @Resource
    private TopicInterfaceStatisticsService statisticsService;
    @Resource
    private InterfaceParamService interfaceParamService;

    @Deprecated
    @PostMapping("/{topicName}/getList")
    public Message getList(@PathVariable(value = "topicName") String topicName, @RequestBody String query) {
        Message ret;
        try {

            JSONObject jsonObject = JSONObject.parseObject(query);
            List<Map<String, Object>> data = topicInterfaceService.getList(topicName, jsonObject);
            ret = new Message(data);
        } catch (JSONException e) {
            ret = new Message(500, "", "json格式错误, 请检查你的json数据", null);
        } catch (Exception e) {
            ret = new Message(e);
        }
        return ret;
    }

    @Deprecated
    @GetMapping("/getCount")
    public String getCount(String topicName, String tableName) {
        Message ret;
        try {
            Integer total = topicInterfaceService.getCount(topicName, tableName);
            ret = new Message(total);
        } catch (Exception e) {
            ret = new Message(e);
        }
        return ret.toJsonNoException();
    }

    @RequestMapping("/{topicName}/{tag}")
    public Message service(@PathVariable String topicName,
                           @PathVariable String tag,
                           HttpServletRequest request) {
        Message mes;
        try {

            List<Map<String, Object>> ret = topicInterfaceService.service(topicName, tag, request);
            mes = new Message(ret);
        } catch (Exception e) {
            mes = new Message(e);
        }
        return mes;

    }

    @Deprecated
    @GetMapping("/getData/{topicName}/{tableName}")
    public Message callInterface(@PathVariable("topicName") String topicName,
                                 @PathVariable("tableName") String tableName,
                                 HttpServletRequest request,
                                 String where,
                                 @RequestParam(value = "page", defaultValue = "1") String page,
                                 @RequestParam(value = "limit", defaultValue = "1000") String limit) {

        Message retMessage;
        try {

            List<Map<String, Object>> retData = topicInterfaceService.callInterface(topicName, tableName, where, page, limit);
            //根据topicName和TableName查询接口id  并在统计中加入一条记录
            String url = TOPIC_URL_PRE + topicName + "/" + tableName;
            String interfaceId = topicInterfaceService.getInterfaceIdByUrl(url);
            String userIp = getUserIP(request);
            TopicInterfaceStatistics statistics = new TopicInterfaceStatistics();
            statistics.setTopicInterfaceId(Long.valueOf(interfaceId));
            statistics.setUserIp(userIp);
            statistics.setCallTime(new Date());
            statistics.setParam(where);
            statisticsService.create(statistics);

            retMessage = new Message(retData);
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage;
    }


}
