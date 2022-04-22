package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.TopicRepository;

public interface TransactionalTestService {
    void insertTest();

    boolean createDBTest();

    void deleteDBTest();
}
