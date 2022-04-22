package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.service.*;
import cn.cnic.zhongtai.utils.JsonUtils;
import cn.cnic.zhongtai.utils.UnixUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/mainpage")
@Api(tags = "统计页")
public class StatisticsPageController {

    @Resource
    private TopicInterfaceService topicInterfaceService;
    @Resource
    private TopicInterfaceStatisticsService statisticsService;
    @Resource
    private TopicService topicService;
    @Resource
    private MapStorageService mapStorageService;
    @Resource
    private GenTableService genTableService;

    @Resource
    private DataManageService dataManageService;

    @GetMapping("/totalCount")
    @ApiOperation(value = "获取总数")
    public String getTotalCount(){

        Map<String, Object> retMap;
        try{
            retMap = new HashMap<>();
            int interfaceCount = topicInterfaceService.getTotalCount();
            int totalCallCount = statisticsService.getTotalCount();
            int topicCount = topicService.getTotalCount();
            int mapCount = mapStorageService.getTotalCount();
            int modelCount =  genTableService.getTotalCount();
            int dataManageCount =   dataManageService.getTotalCount();
            retMap.put("code", 200);
            retMap.put("interfaceCount", interfaceCount);
            retMap.put("totalCallCount", totalCallCount);
            retMap.put("topicCount", topicCount);
            retMap.put("mapCount", mapCount);
            retMap.put("modelCount", modelCount);
            retMap.put("dataManageCount", dataManageCount);

        }catch (Exception e){
            retMap = new HashMap<>();
            retMap.put("code", 500);
            retMap.put("errMsg", e.getLocalizedMessage());
        }
        return JsonUtils.toJsonNoException(retMap);
    }

    @GetMapping("/systeminfo")
    @ApiOperation(value = "获取系统信息")
    public String systeminfo(){

        Map<String, Object> retMap;
        try{
            retMap = new HashMap<>();
            int[] memInfo = UnixUtils.getMemInfo();
            UnixUtils.Desk deskUsage = UnixUtils.getDeskUsage();
            Map<String, Object> mem = new HashMap<>();
            mem.put("MemTotal", memInfo[0]);
            mem.put("MemFree", memInfo[1]);
            mem.put("MemAvailable", memInfo[2]);
            retMap.put("mem", mem);

            Map<String, Object> disk = new HashMap<>();
            disk.put("DiskTotal", deskUsage.getTotal());
            disk.put("DiskUsed", deskUsage.getUsed());
            disk.put("DiskAvailable", deskUsage.getAvailable());
            retMap.put("disk", disk);

            retMap.put("code", 200);

        }catch (Exception e){
            retMap = new HashMap<>();
            retMap.put("code", 500);
            retMap.put("errMsg", e.getLocalizedMessage());
        }
        return JsonUtils.toJsonNoException(retMap);
    }
}
