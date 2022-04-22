package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.MapPortMapper;
import cn.cnic.zhongtai.system.model.MapPort;
import cn.cnic.zhongtai.system.service.MapPortService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
public class MapPortServiceImpl implements MapPortService {

    @Resource
    private MapPortMapper mapPortMapper;
    @Override
    public Integer getMaxPort() {
        return mapPortMapper.getMaxPort();
    }

    @Override
    public void create(MapPort mapPort) {
        mapPortMapper.create(mapPort);
    }
}
