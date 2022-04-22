package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.TopicInterface;

import java.util.List;

public interface TopicInterfaceMapper {

    void create(TopicInterface topicInterface);

    List<TopicInterface> getAllByTopicName(String topicName);

    void deleteByTopicName(String topicName);

    String getInterfaceIdByUrl(String url);

    int getTotalCount();

    Integer checkStatus(String url);

    TopicInterface getByUrl(String url);
}
