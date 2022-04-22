package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.InterfaceParamMapper;
import cn.cnic.zhongtai.system.model.InterfaceParam;
import cn.cnic.zhongtai.system.service.InterfaceParamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
public class InterfaceParamServiceImpl implements InterfaceParamService {
    @Resource
    private InterfaceParamMapper interfaceParamMapper;
    @Override
    public Integer insert(InterfaceParam param) {
        return interfaceParamMapper.insert(param);
    }

    @Override
    public InterfaceParam getByUrl(String url) {
        return interfaceParamMapper.getByUrl(url);
    }

    @Override
    public List<InterfaceParam> getListByTopicName(String topicName) {
        return interfaceParamMapper.getListByTopicName(topicName);
    }
}
