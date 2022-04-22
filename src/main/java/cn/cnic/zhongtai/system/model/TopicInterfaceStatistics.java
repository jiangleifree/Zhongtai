package cn.cnic.zhongtai.system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

@Data
public class TopicInterfaceStatistics {

    @JsonIgnore
    private transient long id;
    @JsonIgnore
    private long topicInterfaceId;
    //调用时间
    private Date callTime;
    //参数
    private String param;
    //访问人IP
    private String userIp;
}
