package cn.cnic.zhongtai.system.model.vo;


import lombok.Data;

@Data
public class DataAddToFtpVo {
    private String ip;

    private int port;

    private String username;

    private String password;

    private String path;

    private String dataType;

    private String tableId;

    private String modelTypeName;

    private String processingMode;

    //虚拟目录的id
    private int dataMappingId;
    //虚拟目录的name
    private String dataMappingName;
}