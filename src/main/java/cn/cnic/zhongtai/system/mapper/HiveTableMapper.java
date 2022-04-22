package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.HiveTable;
import cn.cnic.zhongtai.system.model.HiveTableExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HiveTableMapper {
    int countByExample(HiveTableExample example);

    int deleteByExample(HiveTableExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(HiveTable record);

    int insertSelective(HiveTable record);

    List<HiveTable> selectByExample(HiveTableExample example);

    HiveTable selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") HiveTable record, @Param("example") HiveTableExample example);

    int updateByExample(@Param("record") HiveTable record, @Param("example") HiveTableExample example);

    int updateByPrimaryKeySelective(HiveTable record);

    int updateByPrimaryKey(HiveTable record);

    //以下是自定义接口
    int insertHiveTableList(List<HiveTable> list);

    List<HiveTable> hiveTableListByTableName(@Param("tableName") String tableName);
}