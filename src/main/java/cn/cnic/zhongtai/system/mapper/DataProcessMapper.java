package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.DataProcess;

import java.util.List;

public interface DataProcessMapper {
    List<DataProcess> getList();
    void insert(DataProcess dataProcess);
    void updateStatusById(Integer id, String status);
}
