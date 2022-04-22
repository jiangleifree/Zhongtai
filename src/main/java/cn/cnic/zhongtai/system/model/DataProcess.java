package cn.cnic.zhongtai.system.model;

import lombok.Data;

import java.util.Date;

@Data
public class DataProcess {
    private Integer id;
    private String name;
    private Date createDate;
    private String status;
    private String type; // 关系提取
}
