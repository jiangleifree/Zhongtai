package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.GenTable;
import cn.cnic.zhongtai.system.model.GenTableExample;
import java.util.List;
import java.util.Map;

import cn.cnic.zhongtai.system.model.TableInfo;
import org.apache.ibatis.annotations.Param;

public interface GenTableMapper {
    int countByExample(GenTableExample example);

    int deleteByExample(GenTableExample example);

    int deleteByPrimaryKey(Long tableId);

    int insert(GenTable record);

    int insertSelective(GenTable record);

    List<GenTable> selectByExample(GenTableExample example);

    GenTable selectByPrimaryKey(Long tableId);

    int updateByExampleSelective(@Param("record") GenTable record, @Param("example") GenTableExample example);

    int updateByExample(@Param("record") GenTable record, @Param("example") GenTableExample example);

    int updateByPrimaryKeySelective(GenTable record);

    int updateByPrimaryKey(GenTable record);

    //以上是默认生成的,从这开始写自定义写接口

    GenTable selectGenTableBytableName(String tableName);

    List<GenTable> queryGenTableList( Map<String,Object> param);

    List<GenTable> selectGenTableList();

    int queryCount();

    boolean deleteTables(@Param("tableIds")List<Integer> tableIds);

    boolean deleteTablesBytableId(@Param("tableId") int tableId);

    List<GenTable> modelListByTableName(@Param("tableName") String tableName);

    GenTable findTableInfoByTableId(long tableId);

    List<GenTable> selectGenTableListByTableName(@Param("tableName") String tableName);

    int getTotalCount();
}