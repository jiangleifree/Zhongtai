package cn.cnic.zhongtai.system.model;

import lombok.Data;
@Data
public class Neo4jTypeProperty {
    private String nodeType;
    private String nodeLabel;
    private String propertyName;
    private String propertyType;
    private boolean mandatory;
}
