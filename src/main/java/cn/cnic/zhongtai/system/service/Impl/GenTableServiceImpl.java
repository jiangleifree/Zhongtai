package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.GenTableColumnMapper;
import cn.cnic.zhongtai.system.mapper.GenTableMapper;
import cn.cnic.zhongtai.system.model.GenTable;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.service.GenTableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;

@Transactional
@Service
@Slf4j
public class GenTableServiceImpl implements GenTableService {


    @Resource
    private GenTableMapper genTableMapper;
    @Resource
    private GenTableColumnMapper genTableColumnMapper;

    public boolean selectGenTableBytableName(String tableName) {
        return null != genTableMapper.selectGenTableBytableName(tableName) ? true : false;
    }


    public boolean insertGenTable(GenTable genTable) {
        int rowList = 0;
        genTable.setCreateTime(new Date());
        genTable.setStatus("0");
        //转小写
        genTable.setTableName(genTable.getTableName().toLowerCase());
        int row = genTableMapper.insert(genTable);
        if (row > 0) {
            List<GenTableColumn> genTableColumnList = genTable.getGenTableColumn();
            if (!genTableColumnList.isEmpty() && !CollectionUtils.isEmpty(genTableColumnList)) {
                for (GenTableColumn genTableColumn : genTableColumnList) {
                    //关联上模型总表的id
                    genTableColumn.setTableId(String.valueOf(genTable.getTableId()));
                    genTableColumn.setCreateTime(new Date());
                    genTableColumn.setStatus("0");
                }
                rowList = genTableColumnMapper.insertGenTableColumnList(genTableColumnList);
            }
        }
        return row > 0 && rowList > 0 ? true : false;
    }

    public List<GenTable> queryGenTableList(int currPage, int pageSize) {
        Map<String, Object> data = new HashMap<>();
        data.put("currIndex", (currPage - 1) * pageSize);
        data.put("pageSize", pageSize);
        return genTableMapper.queryGenTableList(data);
    }

    public List<GenTable> selectGenTableList() {
        return genTableMapper.selectGenTableList();
    }

    @Override
    public List<GenTable> selectGenTableListByTableName(String tableName) {
        return genTableMapper.selectGenTableListByTableName(tableName);
    }

    public int queryCount() {
        return genTableMapper.queryCount();
    }


    public boolean deleteTables(String tableId) {
        if (StringUtils.isNotBlank(tableId) && tableId.contains(",") && tableId.split(",").length > 0) {
            String[] tableIds = tableId.split(",");
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < tableIds.length; i++) {
                if (!"".equals(tableIds[i]))
                    list.add(Integer.parseInt(tableIds[i]));
            }
            boolean delGenTables = genTableMapper.deleteTables(list);
            boolean delGenTableColumns = genTableColumnMapper.deleteTableColumnsByTableIds(list);
            return delGenTables && delGenTableColumns ? true : false;
        } else {
            boolean delGenTable = genTableMapper.deleteTablesBytableId(Integer.parseInt(tableId));
            boolean delGenTableColumn = genTableColumnMapper.deleteTableColumnBytableId(tableId);
            return delGenTable && delGenTableColumn ? true : false;
        }
    }

    public List<GenTable> modelListByTableName(String tableName) {
        List<GenTable> list = genTableMapper.modelListByTableName(tableName);
        return !list.isEmpty() && null != list ? list : null;
    }

    @Override
    public GenTable findTableInfoByTableId(long tableId) {
        return genTableMapper.findTableInfoByTableId(tableId);
    }


    @Override
    public int getTotalCount() {
        return genTableMapper.getTotalCount();
    }


    @Override
    public GenTable selectByPrimaryKey(Long tableId){
        return genTableMapper.selectByPrimaryKey(tableId);
    }

}
