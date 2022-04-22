package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.GenTable;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.model.Message;
import cn.cnic.zhongtai.system.service.GenTableService;
import cn.cnic.zhongtai.utils.JsonUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 整体模型控制类
 * Created  by wdd on 2019/10/16
 **/
@RestController
@RequestMapping(value = "/genTable")
@Api(tags = "模型管理")
@Slf4j
public class GenTableController {

    @Resource
    private GenTableService genTableService;

    @GetMapping("/findTableInfoByTableId")
    @ApiOperation(value = "根据tableId查询模型信息", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tableId", value = "模型的id", required = true)
    })
    public String findTableInfoByTableId(String tableId) {
        Message retMessage;
        try {
            if(StringUtils.isEmpty(tableId)){
                throw new RuntimeException("tableId must not be null");
            }
            GenTable tableInfo = genTableService.findTableInfoByTableId(Long.valueOf(tableId));
            retMessage = new Message(tableInfo);

        }catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    /**
     * 添加模型功能
     * @param genTable
     * @return
     */
    @RequestMapping(value = "/genTable_add", method = RequestMethod.POST)
    public String genTable_add(GenTable genTable) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        List<GenTableColumn> genTableColumnList = genTable.getGenTableColumn();

        //先检查模型名称和模型内容是否为空
        if (StringUtils.isBlank(genTable.getTableName()) && genTableColumnList.isEmpty()) {
            rtnMap.put("errMsg", "参数为空,提交错误！");
            return JsonUtils.toJsonNoException(rtnMap);
        }
        //校验模型名称是否存在
        if (genTableService.selectGenTableBytableName(genTable.getTableName())) {
            rtnMap.put("errMsg", "模型名称已存在！");
            return JsonUtils.toJsonNoException(rtnMap);
        }
        //开始入库,先保存模型总表
        if (genTableService.insertGenTable(genTable)) {
            rtnMap.put("code", 200);
            rtnMap.put("errMsg", "模型创建成功！");
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }


    /**
     * 模型管理列表信息
     *
     * @param page
     * @param limit
     * @return
     */
    @RequestMapping(value = "/model_list", method = RequestMethod.GET)
    @ApiOperation(value = "模型管理列表信息", notes = "模型管理列表信息,分页查询", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "分页的起始页", required = true, dataType = "String"),
            @ApiImplicitParam(name = "limit", value = "每页显示的数量", required = true, dataType = "String")
    })
    public String modellist(@RequestParam(value = "page", defaultValue = "1") String page, @RequestParam(value = "limit", defaultValue = "10") String limit) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        //当前页
        Integer currPage = Integer.parseInt(page.trim());
        //每页的数量
        Integer pageSize = Integer.parseInt(limit.trim());
        PageHelper.startPage(currPage, pageSize, true);
        List<GenTable> GenTableList = genTableService.selectGenTableList();
        PageInfo<GenTable> pageInfo = new PageInfo<>(GenTableList);

        try{
            rtnMap.put("msg", "success");
            rtnMap.put("code", "0");
            rtnMap.put("data", GenTableList);
            rtnMap.put("count", pageInfo.getTotal());
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            rtnMap.put("msg", "error");
            rtnMap.put("code", "222222");
        }

        return JsonUtils.toJsonNoException(rtnMap);

    }

    /**
     * 模型总表信息
     *
     * @param page
     * @param limit
     * @return
     */
    @RequestMapping(value = "/model_list_by_table_name", method = RequestMethod.GET)
    @ApiOperation(value = "根据名称模糊查询模型基本信息", notes = "根据名称模糊查询模型基本信息,分页查询", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "分页的起始页",  dataType = "String"),
            @ApiImplicitParam(name = "limit", value = "每页显示的数量", dataType = "String"),
            @ApiImplicitParam(name = "tableName", value = "表名", dataType = "String")
    })
    public String modelListByTableName(@RequestParam(value = "page", defaultValue = "1") String page,
                                       @RequestParam(value = "limit", defaultValue = "10") String limit,
                                       @RequestParam(value = "tableName", defaultValue = "") String tableName) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        //当前页
        Integer currPage = Integer.parseInt(page.trim());
        //每页的数量
        Integer pageSize = Integer.parseInt(limit.trim());
        PageHelper.startPage(currPage, pageSize, true);
        List<GenTable> GenTableList = genTableService.selectGenTableListByTableName(tableName);
        PageInfo<GenTable> pageInfo = new PageInfo<>(GenTableList);

        try{
            rtnMap.put("msg", "success");
            rtnMap.put("code", "0");
            rtnMap.put("data", GenTableList);
            rtnMap.put("count", pageInfo.getTotal());
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            rtnMap.put("msg", "error");
            rtnMap.put("code", "222222");
        }

        return JsonUtils.toJsonNoException(rtnMap);

    }

    /**
     * 删除操作,逻辑删除
     *
     * @param tableId
     * @return
     */
    @RequestMapping(value = "/deleteTables", method = RequestMethod.POST)
    @ApiOperation(value = "模型删除", notes = "根据id进行删除(逻辑删除),支持批量删除,批量使用,分割", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tableId", value = "模型id", dataType = "String",required = true)
    })
    public String deleteTable(String tableId) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        if (StringUtils.isNotBlank(tableId)) {
            try {
                genTableService.deleteTables(tableId);
                rtnMap.put("code", 200);
                rtnMap.put("errMsg", "删除成功");
            }catch (Exception e) {
                log.error("删除模型"+tableId+"错误",e);
                rtnMap.put("errMsg", "删除异常");
            }
        } else {
            rtnMap.put("errMsg", "参数错误,删除失败");
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }

}
