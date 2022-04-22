package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.InterfaceParam;
import cn.cnic.zhongtai.system.model.TableInfo;
import cn.cnic.zhongtai.system.model.TopicRepository;

import java.util.List;
import java.util.Map;

public interface TopicService {

    List<TopicRepository> getAllTopic();
    void createTopic(TopicRepository topicRepository);
    TopicRepository findTopicByTopicName(String topicName);

    void deleteTopic(String topicName);

    List<Map<String, Object>> getAllDataByTableName(String topicName, String tableName);

    List<String> getAllFieldsByDBNameAndTableName(String topicName, String tableName);

    TableInfo getTableInfoByTopicAndTableName(String topicName, String tableName);

    boolean authentication(String topicName, String userName, String password);

    int getTotalCount();

    List<Map<String, String>> testSql(String topicName, String sql);

    void createInterface(InterfaceParam param);

    List<String> getAllFieldsByTopicName(String topicName);

    Map<String, Object> getTopicInterfaceInfo(String topicName);
}
