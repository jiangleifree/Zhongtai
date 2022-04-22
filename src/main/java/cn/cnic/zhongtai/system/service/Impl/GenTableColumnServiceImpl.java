package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.GenTableColumnMapper;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.service.GenTableColumnService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Transactional
@Service
@Slf4j
public class GenTableColumnServiceImpl implements GenTableColumnService {

    @Resource
    private GenTableColumnMapper genTableColumnMapper;


    public List<GenTableColumn>  selectModelColumnByTableId(String tableId){
        return genTableColumnMapper.selectModelColumnByTableId(tableId);
    }

    @Override
    public void insertGenTableColumnList(List<GenTableColumn> list) {
        genTableColumnMapper.insertGenTableColumnList(list);
    }


}
