package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.vo.GenTableVO;

public interface SqlDaoService {
    boolean createDB(String dbName);
    void deleteDB(String dbName);
    boolean createTable(String dbName, GenTableVO table);
}
