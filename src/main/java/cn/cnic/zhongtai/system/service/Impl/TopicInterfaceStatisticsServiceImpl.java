package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.TopicInterfaceStatisticsMapper;
import cn.cnic.zhongtai.system.model.TopicInterfaceStatistics;
import cn.cnic.zhongtai.system.service.TopicInterfaceStatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
public class TopicInterfaceStatisticsServiceImpl implements TopicInterfaceStatisticsService {

    @Resource
    private TopicInterfaceStatisticsMapper mapper;

    @Override
    public void create(TopicInterfaceStatistics statistics) {
        mapper.create(statistics);
    }

    @Override
    public List<TopicInterfaceStatistics> getListByInterfaceId(String interfaceId) {
        return mapper.getListByInterfaceId(interfaceId);
    }

    @Override
    public int getTotalCount() {
        return mapper.getTotalCount();
    }
}
