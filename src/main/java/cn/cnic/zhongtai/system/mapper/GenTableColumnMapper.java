package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.GenTable;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.model.GenTableColumnExample;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

public interface GenTableColumnMapper {
    int countByExample(GenTableColumnExample example);

    int deleteByExample(GenTableColumnExample example);

    int deleteByPrimaryKey(Long columnId);

    int insert(GenTableColumn record);

    int insertSelective(GenTableColumn record);

    List<GenTableColumn> selectByExample(GenTableColumnExample example);

    GenTableColumn selectByPrimaryKey(Long columnId);

    int updateByExampleSelective(@Param("record") GenTableColumn record, @Param("example") GenTableColumnExample example);

    int updateByExample(@Param("record") GenTableColumn record, @Param("example") GenTableColumnExample example);

    int updateByPrimaryKeySelective(GenTableColumn record);

    int updateByPrimaryKey(GenTableColumn record);

    //自定义开始
    int insertGenTableColumnList(List<GenTableColumn> list);

    List<GenTableColumn> selectModelColumnByTableId(@Param("tableId") String tableId);

    boolean deleteTableColumnsByTableIds(@Param("tableIds")List<Integer> tableIds);

    boolean deleteTableColumnBytableId(@Param("tableId") String tableId);
}