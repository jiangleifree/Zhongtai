package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.MapPort;

public interface MapPortMapper {
    Integer getMaxPort();

    void create(MapPort mapPort);

    void deleteByMapNameEn(String mapNameEn);
}
