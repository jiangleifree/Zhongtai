package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.vo.SqlVo;

import java.util.List;

public interface DataImportMapper {

    List<Integer> selectBysql(SqlVo sql);
}