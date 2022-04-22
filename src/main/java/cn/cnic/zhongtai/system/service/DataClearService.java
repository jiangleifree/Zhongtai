package cn.cnic.zhongtai.system.service;


import cn.cnic.zhongtai.system.model.ColumnSimpleInfo;
import cn.cnic.zhongtai.system.model.vo.DataClearVO;

import java.util.List;
import java.util.Map;

public interface DataClearService {


    Map<String, Object> runJobToClearData(DataClearVO dataClearVO);

    /**
     * 分词接口
     */
    Map<String,Object> participleeColumn(DataClearVO dataClearVO);

    /**
     * 数据统计分析
     * @return
     */
    void statistical_Information(String tableName,List<ColumnSimpleInfo> columnList );

    }
