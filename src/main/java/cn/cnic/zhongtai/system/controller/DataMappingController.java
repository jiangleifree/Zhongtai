package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.DataMapping;
import cn.cnic.zhongtai.system.model.Message;
import cn.cnic.zhongtai.system.model.vo.TreeNodeVo;
import cn.cnic.zhongtai.system.service.DataMappingService;
import cn.cnic.zhongtai.utils.JsonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 数据管理虚拟目录
 * Created  by wdd on 2019/11/21
 **/
@Controller
@RequestMapping(value = "/dataMapping")
@Slf4j
@Api(value = "数据管理", tags = "数据管理(虚拟目录)")
public class DataMappingController {


    @Resource
    private DataMappingService dataMappingService;


    /**
     * 查询所有
     *
     * @return
     */
    @RequestMapping(value = "getDataMappingList")
    @ResponseBody
    @ApiOperation(value = "查询所有", response = String.class)
    public String getDataMappingList(String name) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        try {
            List<DataMapping> list = dataMappingService.getDataMappingList(name);
            rtnMap.put("data", list);
            rtnMap.put("code", 200);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            rtnMap.put("msg", "error");
            rtnMap.put("code", 500);
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }


    /**
     * 根据父id查询下级数据
     *
     * @return
     */
    @RequestMapping(value = "getDataMappingListByPid")
    @ResponseBody
    @ApiOperation(value = "根据父id查询下级数据", response = String.class)
    public String getDataMappingListByPid(int parent_id) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        try {
            List<DataMapping> list = dataMappingService.getDataMappingListByPid(parent_id);
            rtnMap.put("data", list);
            rtnMap.put("code", 200);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            rtnMap.put("msg", "error");
            rtnMap.put("code", 500);
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }

    /**
     * 根据父id查询下级数据
     *
     * @return
     */
    @GetMapping(value = "getDataMappingTableNamesByPid")
    @ResponseBody
    @ApiOperation(value = "根据父id查询下级数据status为可用表名", response = String.class)
    public String getDataMappingTableNamesByPid(int parent_id) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        try {
            List<DataMapping> list = dataMappingService.getDataMappingListByPid(parent_id);
            List<String> tableNames = list.stream()
                    .filter(dataMapping -> dataMapping.getType() == 1)
                    .filter(dataMapping -> dataMapping.getStatus().equals("0"))
                    .map(dataMapping -> dataMapping.getName())
                    .collect(Collectors.toList());
            rtnMap.put("data", tableNames);
            rtnMap.put("code", 200);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            rtnMap.put("msg", "error");
            rtnMap.put("code", 500);
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }


    @RequestMapping(value = "insert")
    @ResponseBody
    public String insert(TreeNodeVo treeNodeVO) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        try {
            DataMapping  dataMapping = dataMappingService.insert(treeNodeVO);
            rtnMap.put("data", dataMapping);
            rtnMap.put("msg", "添加成功");
            rtnMap.put("code", 200);
        } catch (Exception e) {
            log.error("添加失败：",e);
            rtnMap.put("msg",e.getMessage());
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }

    @RequestMapping(value = "update")
    @ResponseBody
    public String update(TreeNodeVo treeNodeVO) {
        try {
            dataMappingService.update(treeNodeVO);
            return new Message<>("修改成功").toJsonNoException();
        } catch (Exception e) {
            e.printStackTrace();
            return new Message<>(e).toJsonNoException();
        }
    }

    @RequestMapping(value = "deleteById")
    @ResponseBody
    public String deleteById(String id) {
        try {
            dataMappingService.deleteById(Integer.parseInt(id));
            return new Message<>("删除成功").toJsonNoException();
        } catch (Exception e) {
            e.printStackTrace();
            return new Message<>(e).toJsonNoException();
        }
    }

    @GetMapping("/getById")
    @ResponseBody
    public String getById(String id){
        Message ret;
        try {
            ret = new Message(dataMappingService.getById(Integer.valueOf(id)));
        } catch (Exception e){
            ret = new Message(e);
        }
        return ret.toJsonNoException();
    }

    @GetMapping("/getTableInfoById")
    @ResponseBody
    public String getTableInfoById(String id){
        Message ret;
        try {
            ret = new Message(dataMappingService.getTableInfoById(Integer.valueOf(id)));
        } catch (Exception e){
            ret = new Message(e);
        }
        return ret.toJsonNoException();
    }

    @GetMapping("/getTableSchemaById")
    @ResponseBody
    public String getTableSchemaById(String id){
        Map<String, Object> ret = new HashMap<>();
        try {
            Object columns = dataMappingService.getTableInfoById(Integer.valueOf(id)).get("columns");
            ret.put("code", 0);
            ret.put("data", columns);
        } catch (Exception e){
            ret.put("code", -1);
        }
        return JsonUtils.toJsonNoException(ret);
    }

    @GetMapping("/testAvailability")
    @ResponseBody
    public String testAvailability(String id){
        Message ret;
        try {
            ret = new Message(dataMappingService.testAvailability(Integer.valueOf(id)));
        } catch (Exception e){
            ret = new Message(e);
        }
        return ret.toJsonNoException();
    }

}
