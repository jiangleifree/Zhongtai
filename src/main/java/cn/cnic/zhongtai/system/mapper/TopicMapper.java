package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.TopicRepository;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TopicMapper {
    List<TopicRepository> getAllTopic();
    int createTopic(TopicRepository topicRepository);
    TopicRepository findTopicByTopicName(String topicName);
    void deleteTopicByTopicName(String topicName);

    String getDBNameByTopicName(String topicName);

    TopicRepository selectTopicByPwd(@Param("topicName") String topicName, @Param("userName") String userName, @Param("password") String password);

    int getTotalCount();

    TopicRepository getByTopicName(String topicName);
}
