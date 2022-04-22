package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.TopicInterfaceStatistics;

import java.util.List;

public interface TopicInterfaceStatisticsService {


    void create(TopicInterfaceStatistics statistics);

    List<TopicInterfaceStatistics> getListByInterfaceId(String interfaceId);

    int getTotalCount();
}
