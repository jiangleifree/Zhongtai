package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.dao.SqlDao;
import cn.cnic.zhongtai.system.model.vo.GenTableVO;
import cn.cnic.zhongtai.system.service.SqlDaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class SqlDaoServiceImpl implements SqlDaoService {

    @Resource
    private SqlDao sqlDao;

    @Override
    @Transactional(value = "mysqlShardTransactionManager", propagation = Propagation.NOT_SUPPORTED)
    public boolean createDB(String dbName) {
        return sqlDao.createDatabase(dbName);
    }

    @Override
    @Transactional(value = "mysqlShardTransactionManager", propagation = Propagation.NOT_SUPPORTED)
    public void deleteDB(String dbName) {
        sqlDao.deleteDatabaseByDBName(dbName);
    }

    @Override
    public boolean createTable(String dbName, GenTableVO table) {
        return sqlDao.createTable(dbName, table);
    }
}
