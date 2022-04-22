package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.TableInfo;

import java.util.List;

public interface TableInfoMapper {
    List<TableInfo> findTablesByTopicName(String topicName);

    int createTableInfo(TableInfo tableVo);
}
