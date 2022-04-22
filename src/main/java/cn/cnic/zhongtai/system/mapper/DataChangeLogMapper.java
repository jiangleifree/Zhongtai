package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.DataChangeLog;
import cn.cnic.zhongtai.system.model.DataChangeLogExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DataChangeLogMapper {
    int countByExample(DataChangeLogExample example);

    int deleteByExample(DataChangeLogExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(DataChangeLog record);

    int insertSelective(DataChangeLog record);

    List<DataChangeLog> selectByExample(DataChangeLogExample example);

    DataChangeLog selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") DataChangeLog record, @Param("example") DataChangeLogExample example);

    int updateByExample(@Param("record") DataChangeLog record, @Param("example") DataChangeLogExample example);

    int updateByPrimaryKeySelective(DataChangeLog record);

    int updateByPrimaryKey(DataChangeLog record);

    List<DataChangeLog> getDataChangeLogByTableName(String tableName);
}