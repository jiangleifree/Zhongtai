package cn.cnic.zhongtai.system.service;


import cn.cnic.zhongtai.system.model.GenTableColumn;

import java.util.List;

public interface GenTableColumnService {

    /**
     * 根据模型总表的id查询模型表的所有字段
     * @return
     */
    List<GenTableColumn> selectModelColumnByTableId(String tableId);

    void insertGenTableColumnList(List<GenTableColumn> list);

}
