package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.Participle;
import cn.cnic.zhongtai.system.model.ParticipleExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ParticipleMapper {
    int countByExample(ParticipleExample example);

    int deleteByExample(ParticipleExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(Participle record);

    int insertSelective(Participle record);

    List<Participle> selectByExample(ParticipleExample example);

    Participle selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") Participle record, @Param("example") ParticipleExample example);

    int updateByExample(@Param("record") Participle record, @Param("example") ParticipleExample example);

    int updateByPrimaryKeySelective(Participle record);

    int updateByPrimaryKey(Participle record);
}