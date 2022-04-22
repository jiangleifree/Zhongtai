package cn.cnic.zhongtai.system.service;


import cn.cnic.zhongtai.system.model.DataMapping;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.model.vo.TreeNodeVo;

import java.util.List;
import java.util.Map;

public interface DataMappingService {

    /**
     * 查询所有
     * @return
     */
    List<DataMapping> getDataMappingList(String name);

    void updateByExampleSelective(DataMapping dataMapping);


    List<DataMapping> getDataMappingListByPid(Integer pid);

    /**
     * 统计数量
     * @return
     */
    int getTotalCount();

    String getHiveTableNameByPidAndName(Integer pid, String name);

    /**
     * 添加节点
     * @param treeNodeVO
     * @return
     */
    DataMapping insert(TreeNodeVo treeNodeVO);

    int insert(DataMapping  dataMapping);

    int update(TreeNodeVo treeNodeVO);

    int update(DataMapping  dataMapping);

    int deleteById(Integer id);

    DataMapping getById(Integer id);

    Map<String, Object> getTableInfoById(Integer id);

    Map<String, Object> testAvailability(Integer id);

    DataMapping selectByTableName(String tableName);
}
