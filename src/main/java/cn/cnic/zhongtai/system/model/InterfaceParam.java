package cn.cnic.zhongtai.system.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class InterfaceParam {
    @Data
    public static class Parameter {
        private String name;
        private String in; //query or body
        private String mapping;
        private String description;
        private String required; //是否必须
        private String type;
        private String javaType;
        private List<Parameter> item; //子集
    }

    /**
     *
     */
    private Integer id;
    private String url;
    private String type;
    private String consumes; //application/json
    private String produces; // "*/*"
    private String topicName;
    private String parametersJsonStr;
    private String responsesJsonStr;
    private String belong; //属于知识图谱还是主题库
    private Date createDate;//创建时间
    private String mapStorageUrl; //图库地址
    /**
     * 标题
     */
    private String title;
    /**
     * 详细描述
     */
    private String summary;
    /**
     * 入参
     */
    private List<Parameter> parameterList;
    /**
     *
     */
    private String sql;
    /**
     * 出参
     */
    private List<Parameter> responseList;
    private List<String> sqlParams;
}
