package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.UniqueTag;

public interface UniqueTagMapper {
    UniqueTag getByName(String tagName);

    void create(UniqueTag uniqueTag);
}
