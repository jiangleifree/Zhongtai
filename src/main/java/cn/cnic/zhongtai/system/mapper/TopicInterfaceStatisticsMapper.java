package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.TopicInterfaceStatistics;

import java.util.List;

public interface TopicInterfaceStatisticsMapper {
    void create(TopicInterfaceStatistics statistics);

    List<TopicInterfaceStatistics> getListByInterfaceId(String interfaceId);

    int getTotalCount();

}
