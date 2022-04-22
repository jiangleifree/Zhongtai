package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.MapPort;

public interface MapPortService {
    Integer getMaxPort();

    void create(MapPort mapPort);
}
