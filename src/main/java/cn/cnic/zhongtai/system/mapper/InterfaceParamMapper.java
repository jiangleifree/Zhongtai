package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.InterfaceParam;

import java.util.Date;
import java.util.List;

public interface InterfaceParamMapper {

    InterfaceParam selectByPrimaryKey(Integer id);
    List<InterfaceParam> getList();
    Integer insert(InterfaceParam param);
    InterfaceParam getByUrl(String url);
    InterfaceParam getByDate(Date createDate, String url);

    List<InterfaceParam> getListByTopicName(String topicName);
}
