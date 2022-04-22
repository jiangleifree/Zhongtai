package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.DataProcess;
import cn.cnic.zhongtai.system.model.Message;
import cn.cnic.zhongtai.system.model.vo.GenTableVO;
import cn.cnic.zhongtai.system.service.DataProcessService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dataProcess")
public class DataProcessController {

    @Resource
    private DataProcessService dataProcessService;

    @PostMapping("/relation/extraction/v1")
    public Message relationExtractionV1(@RequestBody GenTableVO genTable){
        Message retMes = null;
        try{
            dataProcessService.relationExtractionV1(genTable);
            retMes = new Message(200, "任务创建成功", "", null);
        }catch (Exception e) {
            retMes = new Message(e);
        }
        return retMes;

    }

    @GetMapping("/getList")
    public Map<String, Object> getList(@RequestParam(defaultValue = "10") Integer limit,
                                       @RequestParam(defaultValue = "1") Integer page){
        Map<String, Object> retMap = new HashMap<>();
        try{
            PageHelper.startPage(page, limit, true);
            List<DataProcess> data = dataProcessService.getList();
            PageInfo<DataProcess> pageInfo = new PageInfo<>(data);
            retMap.put("code", 0);
            retMap.put("data", data);
            retMap.put("total", pageInfo.getTotal());

        } catch (Exception e) {
            retMap.put("code", 500);
            retMap.put("errMsg", e.getLocalizedMessage());
        }
        return retMap;
    }
}
