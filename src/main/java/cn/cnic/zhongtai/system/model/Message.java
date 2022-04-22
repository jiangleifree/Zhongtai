package cn.cnic.zhongtai.system.model;

import cn.cnic.zhongtai.utils.JsonUtils;

public class Message<T> {
    private String msg;
    private int code;
    private String errMsg;
    private T data;

    // 按照模块定义CodeMsg

    public Message() {
        this(200, "", "", null);
    }

    public Message(T data){
        this(200, "", "", data);
    }

    public Message(Exception e) {
        this(500, "", e.getMessage(), null);
    }

    public Message(int code, String msg, String errMsg, T data) {
        this.code = code;
        this.msg = msg;
        this.errMsg = errMsg;
        this.data = data;
    }

    public String toJsonNoException(){
        return JsonUtils.toJsonNoException(this);
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
