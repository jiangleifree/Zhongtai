package cn.cnic.zhongtai.system.model;

import lombok.Data;

@Data
public class TopicInterface {

    private long id;
    private String url;
    private String topicName;
    private String tableName;
    private String type;
    private Integer status;
    private Integer topicId;
}
