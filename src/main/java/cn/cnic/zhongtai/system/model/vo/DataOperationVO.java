package cn.cnic.zhongtai.system.model.vo;


public class DataOperationVO {

    //处理字段
    private String column;

    //数据处理类型
    private String type; //比如 repalce,split等
    //处理方式:某个类型中的什么方式
    private String mode;

    //标记 ：比如 拆分,根据什么标记进行拆分
    private String sign;

    //长度  比如拆分长度
    private String length;

    //内容
    private String content;

    /**
     * 前缀
     */
    private String prefix;

    /**
     * 后缀
     */
    private String suffix;

    /**
     * 替换后的字段:","分割
     */
    private String split_column;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getSplit_column() {
        return split_column;
    }

    public void setSplit_column(String split_column) {
        this.split_column = split_column;
    }


}