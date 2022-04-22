package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.DataProcess;
import cn.cnic.zhongtai.system.model.vo.GenTableVO;
import java.util.List;

public interface DataProcessService {
    void relationExtractionV1(GenTableVO genTable);
    List<DataProcess> getList();
    void updateStatusById(Integer id, String status);
    void insert(DataProcess dataProcess);
}
