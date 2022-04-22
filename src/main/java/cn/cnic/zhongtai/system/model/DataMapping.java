package cn.cnic.zhongtai.system.model;

import lombok.Data;

import java.util.Date;

@Data
public class DataMapping {
    private Integer id;

    private String name;

    private String tableName;

    private Integer parentId;

    private Integer orderNum;

    private Date createTime;

    private String status;

    private Date updateTime;

    private String remarks;

    private Integer type;

    private Integer modelId;

    //文件和文件夹样式返回
    private String iconClass;
    private String ficonClass;


}