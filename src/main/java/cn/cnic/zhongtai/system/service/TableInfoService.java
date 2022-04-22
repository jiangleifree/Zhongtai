package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.TableInfo;

import java.util.List;

public interface TableInfoService {
    List<TableInfo> findTablesByTopicName(String topicName);
}
