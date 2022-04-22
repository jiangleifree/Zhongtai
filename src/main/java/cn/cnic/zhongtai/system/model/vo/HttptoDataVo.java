package cn.cnic.zhongtai.system.model.vo;


import lombok.Data;

import java.util.List;

@Data
public class HttptoDataVo {

    //操作方式：接口测试(test)/完成入库操作(getToDb)
    private String operationMode;
    //解析返回数据的头结构
    private String dataTitleParam;

    //接入的名称
    private String importName;

    //虚拟目录的id
    private int pid;
    //虚拟目录的name
    private String dataMappingName;
    //url
    private String url;
    private String hts;
    //请求方式
    private String type;
    //编码
    private String charset;

    private boolean params_box;
    private boolean header_box;
    private boolean cookie_box;
    private boolean proxy_box;

    //参数类型：tab_json/tab_batch/tab_kv
    private String parms_tab;

    //cookie
    private String cookies;


    //批量参数
    private String batchParms;

    //json参数
    private String batchJson;

    //参数设置k-v
    private List<paramData> kvParms;

    //头部参数
    private List<paramData> kvHeads;

    //代理
    private Proxy proxy;

    @Data
    public static class paramData {
        private String key;
        private String value;
    }

    @Data
    public static class Proxy {
        private String ip;
        private String port;
    }


}