package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.UniqueTagMapper;
import cn.cnic.zhongtai.system.model.UniqueTag;
import cn.cnic.zhongtai.system.service.UniqueTagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
public class UniqueTagServiceImpl implements UniqueTagService {

    @Resource
    private UniqueTagMapper uniqueTagMapper;
    @Override
    public UniqueTag getByName(String tagName) {
        return uniqueTagMapper.getByName(tagName);
    }

    @Override
    public void create(UniqueTag uniqueTag) {
        uniqueTagMapper.create(uniqueTag);
    }
}
