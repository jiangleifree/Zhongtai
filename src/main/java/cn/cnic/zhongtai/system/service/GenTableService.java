package cn.cnic.zhongtai.system.service;


import cn.cnic.zhongtai.system.model.GenTable;

import java.util.List;

public interface GenTableService {


    /**
     * 验证tableName是否唯一
     * @param tableName
     * @return
     */
    boolean selectGenTableBytableName(String tableName);


    /**
     * 模型总表添加
     * @param genTable
     * @return
     */
    boolean insertGenTable(GenTable genTable);

    /**
     * 分页查询模型总表
     * @param currPage
     * @param pageSize
     * @return
     */
    List<GenTable> queryGenTableList(int currPage, int pageSize);


    /**
     * 查询模型总数
     *
     * @return
     */
    int queryCount();


    /**
     * 查询所有
     * @return
     */
    List<GenTable> selectGenTableList();

    /**
     * 查询所有通过TableName
     * @return
     */
    List<GenTable> selectGenTableListByTableName(String tableName);

    /**
     * 删除(逻辑删除)
     * @param tableId
     * @return
     */
    boolean deleteTables(String tableId);


    List<GenTable> modelListByTableName(String tableName);


    GenTable findTableInfoByTableId(long tableId);

    GenTable selectByPrimaryKey(Long tableId);


    /**
     * 统计数量
     * @return
     */
    int getTotalCount();

}
