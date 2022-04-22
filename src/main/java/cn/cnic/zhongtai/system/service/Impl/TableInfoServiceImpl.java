package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.TopicMapper;
import cn.cnic.zhongtai.system.model.TableInfo;
import cn.cnic.zhongtai.system.service.MysqlSchemaService;
import cn.cnic.zhongtai.system.service.TableInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TableInfoServiceImpl implements TableInfoService {


    @Resource
    private MysqlSchemaService mysqlSchemaService;
    @Resource
    private TopicMapper topicMapper;

    @Override
    public List<TableInfo> findTablesByTopicName(String topicName) {
        String dbName = topicMapper.getDBNameByTopicName(topicName);
        if (StringUtils.isEmpty(dbName)) {
            throw new RuntimeException("主题库对应的db异常");
        }
        return mysqlSchemaService.getAllTablesInfo(dbName);
    }
}
