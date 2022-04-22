package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.InterfaceParam;

import java.util.List;

public interface InterfaceParamService {
    Integer insert(InterfaceParam param);
    InterfaceParam getByUrl(String url);

    List<InterfaceParam> getListByTopicName(String topicName);
}
