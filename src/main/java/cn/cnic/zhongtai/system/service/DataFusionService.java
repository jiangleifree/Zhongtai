package cn.cnic.zhongtai.system.service;

import com.alibaba.fastjson.JSONObject;

public interface DataFusionService {

    void startFusion(JSONObject data);

    void doSuccessTask(String tableName, JSONObject data);
}
