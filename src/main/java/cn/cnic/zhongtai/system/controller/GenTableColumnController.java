package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.service.GenTableColumnService;
import cn.cnic.zhongtai.utils.JsonUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
*
 * 模型控制类
* Created  by wdd on 2019/10/16
*
**/
@RestController
@RequestMapping(value = "/genTableColumn")
@Api(value = "模型属性信息", tags = "模型具体属性信息")
public class GenTableColumnController {


    @Resource
    private GenTableColumnService genTableColumnService;

    @RequestMapping(value = "/modelColumnByTableId" , method = RequestMethod.POST)
    @ApiOperation(value = "查询模型属性信息", notes = "根据模型的id查询模型属性信息,分页查询", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "分页的起始页", required = true, dataType = "String"),
            @ApiImplicitParam(name = "limit", value = "每页显示的数量", required = true, dataType = "String"),
            @ApiImplicitParam(name = "tableId", value = "模型id", required = true, dataType = "String")
    })
    public String modelColumnByTableId( @RequestParam(value="page",defaultValue="1") int page, @RequestParam (value="limit",defaultValue="10") int limit,String tableId) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();

        PageHelper.startPage(page, limit, true);
        List<GenTableColumn> GenTableList = genTableColumnService.selectModelColumnByTableId(tableId);
        PageInfo<GenTableColumn> pageInfo=new PageInfo<>(GenTableList);

        if(!GenTableList.isEmpty()) {
            rtnMap.put("msg", "success");
            rtnMap.put("code", "0");
            rtnMap.put("data", pageInfo.getList());
            rtnMap.put("count", pageInfo.getTotal());
        }else {
            rtnMap.put("msg", "error");
            rtnMap.put("code", "222222");
        }
        return JsonUtils.toJsonNoException(rtnMap);

    }

    @GetMapping(value = "/modelColumnByTableId/{tableId}")
    public String modelColumnByTableId(@PathVariable("tableId") String tableId) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        List<GenTableColumn> GenTableList = genTableColumnService.selectModelColumnByTableId(tableId);
        if(!GenTableList.isEmpty()) {
            rtnMap.put("msg", "success");
            rtnMap.put("code", "0");
            rtnMap.put("data", GenTableList);
        }else {
            rtnMap.put("msg", "error");
            rtnMap.put("code", "222222");
        }
        return JsonUtils.toJsonNoException(rtnMap);

    }

}
