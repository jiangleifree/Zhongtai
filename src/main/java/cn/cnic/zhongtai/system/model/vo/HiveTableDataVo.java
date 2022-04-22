package cn.cnic.zhongtai.system.model.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class HiveTableDataVo {

    private List<Map<String, Object>> data;
    private String total;
    private List<String> columns;
}
