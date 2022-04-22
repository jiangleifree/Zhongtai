package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.dao.SqlDao;
import cn.cnic.zhongtai.system.mapper.TopicInterfaceMapper;
import cn.cnic.zhongtai.system.mapper.TopicMapper;
import cn.cnic.zhongtai.system.model.TopicInterface;
import cn.cnic.zhongtai.system.model.TopicRepository;
import cn.cnic.zhongtai.system.service.TransactionalTestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class TransactionalTestServiceImpl implements TransactionalTestService {
    @Resource
    private TopicMapper topicMapper;
    @Resource
    private TopicInterfaceMapper topicInterfaceMapper;
    @Resource
    private SqlDao sqlDao;
    @Resource
    private TransactionalTestService transactionalTestService;

    @Override
    @Transactional
    public void insertTest() {

        //sqlDao.createDatabase("test1224");
        transactionalTestService.createDBTest();

        TopicInterface topicInterface = new TopicInterface();
        topicInterface.setTopicId(11);
        topicInterface.setTopicName("test");
        topicInterface.setType("GET");
        topicInterface.setUrl("test");

        TopicRepository repository = new TopicRepository();
        repository.setTopicName("test");
        repository.setCreateTime(new Date());
        repository.setDbName("sss");
        repository.setComment("test");
        repository.setAttarchId("test");
        repository.setUserName("test");
        repository.setPassword("test");

        try{
            topicMapper.createTopic(repository);
            topicInterfaceMapper.create(topicInterface);

            if (true)
                throw new RuntimeException("test1");
        } catch (Exception e) {
            //sqlDao.deleteDatabaseByDBName("test1224");
            transactionalTestService.deleteDBTest();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    @Transactional(value = "mysqlShardTransactionManager", propagation = Propagation.NOT_SUPPORTED)
    public boolean createDBTest() {
        boolean test1234 = sqlDao.createDatabase("test1234");
        return test1234;
    }

    @Override
    @Transactional(value = "mysqlShardTransactionManager", propagation = Propagation.NOT_SUPPORTED)
    public void deleteDBTest() {
        sqlDao.deleteDatabaseByDBName("test1234");
    }

}
