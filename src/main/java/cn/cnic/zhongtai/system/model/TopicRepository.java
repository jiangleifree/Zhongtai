package cn.cnic.zhongtai.system.model;

import cn.cnic.zhongtai.system.model.vo.GenTableVO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class TopicRepository {

    //主题ID
    private String topicId;
    //主题库名称
    private String topicName;
    //主题库描述
    private String comment;
    //主题库创建时间
    @JsonFormat(locale="zh", timezone="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    //主题库对应的数据库db
    private String dbName;
    //主题库账号
    private String userName;
    //密码
    private String password;
    //图片在文件服务的id
    private String attarchId;
    //表信息
    private List<GenTableVO> tables;

}
