package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.DataMappingMapper;
import cn.cnic.zhongtai.system.model.DataMapping;
import cn.cnic.zhongtai.system.model.DataMappingExample;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.model.vo.TreeNodeVo;
import cn.cnic.zhongtai.system.service.DataMappingService;
import cn.cnic.zhongtai.system.service.GenTableColumnService;
import cn.cnic.zhongtai.system.service.GenTableService;
import cn.cnic.zhongtai.system.service.HiveService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;


@Service
@Slf4j
@Transactional(value = "mysqlShardTransactionManager")
public class DataMappingServiceImpl implements DataMappingService {

    @Resource
    private  DataMappingMapper DataMappingMapper;
    @Resource
    private GenTableColumnService genTableColumnService;

    @Resource
    private HiveService hiveService;

    @Resource
    private GenTableService genTableService;

    public List<DataMapping> getDataMappingList(String name){
        return  DataMappingMapper.getDataMappingList(name);
    }

    @Override
    public void updateByExampleSelective(DataMapping dataMapping) {

        DataMappingExample example = new DataMappingExample();
        DataMappingExample.Criteria criteria = example.createCriteria();
        criteria.andTableNameEqualTo(dataMapping.getTableName());
        DataMappingMapper.updateByExampleSelective(dataMapping, example);
    }

    @Override
    public List<DataMapping> getDataMappingListByPid(Integer pid){
        return  DataMappingMapper.getDataMappingListByPid(pid);
    }




    @Override
    public int getTotalCount() {
        return DataMappingMapper.getTotalCount();
    }

    @Override
    public String getHiveTableNameByPidAndName(Integer pid, String name) {
        DataMappingExample example = new DataMappingExample();
        DataMappingExample.Criteria criteria = example.createCriteria();
        criteria.andNameEqualTo(name);
        criteria.andParentIdEqualTo(pid);
        List<DataMapping> dataMappings = DataMappingMapper.selectByExample(example);
        return CollectionUtils.isEmpty(dataMappings) ? null : dataMappings.get(0).getTableName();
    }

    @Override
    public DataMapping insert(TreeNodeVo treeNodeVO) {
        DataMapping dm =  DataMappingMapper.selectByPrimaryKey(treeNodeVO.getParentId());
        if (1 == dm.getType()){
            throw new RuntimeException("请选择一个文件夹创建新的文件");
        }
        DataMapping dataMapping = new DataMapping();
        dataMapping.setCreateTime(new Date());
        dataMapping.setUpdateTime(new Date());
        dataMapping.setName(treeNodeVO.getContext());
        dataMapping.setParentId(treeNodeVO.getParentId());
        dataMapping.setStatus("0");
        dataMapping.setType(0);
        DataMappingMapper.insert(dataMapping);
        return dataMapping;
    }

    @Override
    public int insert(DataMapping dataMapping) {
        return DataMappingMapper.insert(dataMapping);
    }

    @Override
    public int update(TreeNodeVo treeNodeVO) {
        DataMapping dataMapping = new DataMapping();
        dataMapping.setUpdateTime(new Date());
        dataMapping.setName(treeNodeVO.getContext());
        dataMapping.setId(Integer.parseInt(treeNodeVO.getNodeId()));
        return DataMappingMapper.updateByPrimaryKeySelective(dataMapping);
    }


    @Override
    public int update(DataMapping dataMapping) {
        dataMapping.setUpdateTime(new Date());
        return DataMappingMapper.updateByPrimaryKeySelective(dataMapping);
    }

    @Override
    public int deleteById(Integer id) {
        delFile(id);
        return 1;
    }

    //递归删除文件
    private void delFile(Integer id){
        List<DataMapping> list = DataMappingMapper.getDataMappingListByPid(id);
        //检查节点是否有下级
        if(CollectionUtils.isNotEmpty(list)){
           for (int i = 0; i < list.size(); i++) {
               delFile(list.get(i).getId());
           }
        }
        //进行delete操作
        DataMapping dataMapping = DataMappingMapper.selectByPrimaryKey(id);
        if (StringUtils.isNotEmpty(dataMapping.getTableName())) {
            //hive中删除表
            hiveService.deleteByName(dataMapping.getTableName());
            //删除对应的model
            Integer modelId = dataMapping.getModelId();
            if (modelId != null) {
                genTableService.deleteTables(String.valueOf(modelId));
            }
        }
        DataMappingMapper.deleteByPrimaryKey(id);
    }

    @Override
    public DataMapping getById(Integer id) {
        return DataMappingMapper.selectByPrimaryKey(id);
    }

    @Override
    public Map<String, Object> getTableInfoById(Integer id) {
        Map<String, Object> ret = new HashMap<>();
        DataMapping dataMapping = DataMappingMapper.selectByPrimaryKey(id);
        String realTableName = dataMapping.getTableName();
        Integer modelId = dataMapping.getModelId();
        ret.put("tableName", dataMapping.getName());
        ret.put("realTableName",realTableName);
        if (modelId != null) {
            List<GenTableColumn> columns = genTableColumnService.selectModelColumnByTableId(String.valueOf(dataMapping.getModelId()));
            for (int i = 0; i < columns.size(); i++) {
                columns.get(i).setHiveTableName(realTableName);
            }
            ret.put("columns", columns);
        } else {
            ret.put("columns", Collections.emptyList());
        }
        ret.put("id", id);
        return ret;
    }

    @Override
    public Map<String, Object> testAvailability(Integer id) {
        Map<String, Object> ret = new HashMap<>();

        DataMapping dataMapping = DataMappingMapper.selectByPrimaryKey(id);
        String hiveTableName = dataMapping.getTableName();
        if (StringUtils.isNotEmpty(hiveTableName)) {
            boolean isExist = hiveService.validateTableNameExistByHive(hiveTableName);
            if (isExist) {
                int count = hiveService.getCountByHiveTableName(hiveTableName);
                ret.put("count", count);
            }
            ret.put("exist", isExist);
        } else {
            throw new RuntimeException("tableName不存在");
        }
        return ret;
    }

    @Override
    public DataMapping selectByTableName(String tableName) {
        return DataMappingMapper.selectByTableName(tableName);
    }
}
