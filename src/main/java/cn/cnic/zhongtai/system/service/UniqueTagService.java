package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.UniqueTag;

public interface UniqueTagService {
    UniqueTag getByName(String tagName);
    void create(UniqueTag uniqueTag);
}
