package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.TopicInterface;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface TopicInterfaceService {
    List<Map<String, Object>> callInterface(String topicName, String tableName, String where);

    List<TopicInterface> getAllByTopicName(String topicName);

    String getInterfaceIdByUrl(String url);

    int getTotalCount();

    List<Map<String, Object>> callInterface(String topicName, String tableName, String where, String page, String limit);

    List<Map<String, Object>> getData(JSONObject jsonObject);

    List<Map<String, Object>> getList(String topicName, JSONObject jsonObject);

    Integer getCount(String topicName, String tableName);

    Boolean checkStatus(String url);

    TopicInterface getByUrl(String url);

    List<Map<String, Object>> service(String topicName, String tag, HttpServletRequest request);
}
