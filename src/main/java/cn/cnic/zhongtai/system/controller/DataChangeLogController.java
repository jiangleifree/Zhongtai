package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.DataChangeLog;
import cn.cnic.zhongtai.system.service.DataChangeLogService;
import cn.cnic.zhongtai.utils.JsonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
*
 * 数据变更记录
* Created  by wdd on 2020/2/17
*
**/
@Controller
@RequestMapping(value = "/dataChangeLog")
@Slf4j
@Api(value = "数据变更", tags = "数据变更记录")
public class DataChangeLogController {


    @Resource
    private DataChangeLogService dataChangeLogService;


    /**
    *
    * 根据table name 查询数据变更记录
    * Created  by wdd on 2020/2/17
    *
    **/
    @RequestMapping(value = "getDataChangeLogByTableName")
    @ResponseBody
    @ApiOperation(value = "根据table name 查询数据变更记录", response = String.class)
    public String getDataChangeLogByTableName(String tableName) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        try {
            List<DataChangeLog> list = dataChangeLogService.getDataChangeLogByTableName(tableName);
            rtnMap.put("data", list);
            rtnMap.put("code", 0);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            rtnMap.put("msg", "error");
            rtnMap.put("code", 500);
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }


}
