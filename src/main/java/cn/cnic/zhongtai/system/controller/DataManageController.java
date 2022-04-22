package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.DataManage;
import cn.cnic.zhongtai.system.service.DataManageService;
import cn.cnic.zhongtai.utils.JsonUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
*
 * 数据管理控制类
* Created  by wdd on 2019/10/24
*
**/
@RestController
@RequestMapping(value = "/dataManage")
@Slf4j
@Api(value = "数据接入", tags = "数据接入列表")
public class DataManageController {


    @Resource
    private  DataManageService DataManageService;




    /**
     * 数据管理列表
     * @param page
     * @param limit
     * @return
     */
    @RequestMapping(value = "/dataManage_list" , method = RequestMethod.POST)
    @ApiOperation(value = "数据接入列表", notes = "数据接入列表信息,分页查询", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "分页的起始页", required = true, dataType = "String"),
            @ApiImplicitParam(name = "limit", value = "每页显示的数量", required = true, dataType = "String"),
            @ApiImplicitParam(name = "type", value = "0：在线收割,1：手动导入", required = false, dataType = "String")
    })
    public String dataManage_list( @RequestParam(value="page",defaultValue="1") String page, @RequestParam (value="limit",defaultValue="10") String limit,@RequestParam(value="type") String type) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
          //当前页
        Integer currPage = Integer.parseInt(page.trim());
        //每页的数量
        Integer pageSize = Integer.parseInt(limit.trim());
        PageHelper.startPage(currPage, pageSize, true);
        List<DataManage> dataManageList = DataManageService.selectDataManageList(type);
        PageInfo<DataManage> pageInfo=new PageInfo<>(dataManageList);

        if(!dataManageList.isEmpty()) {
            rtnMap.put("msg", "success");
            rtnMap.put("code", "0");
            rtnMap.put("data", dataManageList);
            rtnMap.put("count", pageInfo.getTotal());
        }else {
            rtnMap.put("msg", "success");
            rtnMap.put("code", "0");
        }
        return JsonUtils.toJsonNoException(rtnMap);

    }

    /**
     * 文件下载
     * @param request
     * @param response
     */
    @RequestMapping(value = "/downloadFile")
    public void downloadFruitsFile(HttpServletRequest request, HttpServletResponse response)  {
        DataManageService.downloadFruitsFile(request,response);
    }


    /**
     * 数据接入删除
     * @param id
     * @return
     */
    @RequestMapping(value = "/deleteDataManageById", method = RequestMethod.POST)
    @ApiOperation(value = "数据接入信息删除", notes = "根据id进行删除(逻辑删除),支持批量删除,批量使用,分割", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "数据接入信息的id", dataType = "String",required = true)
    })
    public String deleteDataManageById(String id) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        if (StringUtils.isNotBlank(id)) {
            try {
                DataManageService.deleteDataManageById(id);
                rtnMap.put("code", 200);
                rtnMap.put("errMsg", "删除成功");
            }catch (Exception e) {
                log.error("删除数据接入记录："+id+"错误",e);
                rtnMap.put("errMsg", "删除异常");
            }
        } else {
            rtnMap.put("errMsg", "参数错误,删除失败");
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }


    /**
     * 数据接入名称修改
     * @param id
     * @param dataName
     * @return
     */
    @RequestMapping(value = "/updateDataManageName", method = RequestMethod.POST)
    @ApiOperation(value = "数据接入名称修改", notes = "根据id进行修改", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "数据接入信息的id", dataType = "String",required = true),
            @ApiImplicitParam(name = "dataName", value = "要修改的名称", dataType = "String",required = true)
    })
    public String updateDataManageName(String id,String dataName) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        if (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(dataName)) {
            try {
                DataManageService.updateDataManageName(id,dataName);
                rtnMap.put("code", 200);
                rtnMap.put("errMsg", "修改成功");
            }catch (Exception e) {
                log.error("修改数据接入名称："+dataName+"错误",e);
                rtnMap.put("errMsg", "修改异常");
            }
        } else {
            rtnMap.put("errMsg", "参数错误,修改失败");
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }


}
