package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.DataMapping;
import cn.cnic.zhongtai.system.model.DataMappingExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DataMappingMapper {
    int countByExample(DataMappingExample example);

    int deleteByExample(DataMappingExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(DataMapping record);

    int insertSelective(DataMapping record);

    List<DataMapping> selectByExample(DataMappingExample example);

    DataMapping selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") DataMapping record, @Param("example") DataMappingExample example);

    int updateByExample(@Param("record") DataMapping record, @Param("example") DataMappingExample example);

    int updateByPrimaryKeySelective(DataMapping record);

    int updateByPrimaryKey(DataMapping record);


    List<DataMapping> getDataMappingListByPid(Integer pid);
    List<DataMapping> getDataMappingList(@Param("name") String name);
    int getTotalCount();

    DataMapping selectByTableName(String tableName);
}