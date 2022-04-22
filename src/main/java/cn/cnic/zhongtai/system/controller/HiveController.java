package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.ColumnSimpleInfo;
import cn.cnic.zhongtai.system.model.GenTable;
import cn.cnic.zhongtai.system.model.GenTableColumn;
import cn.cnic.zhongtai.system.model.Message;
import cn.cnic.zhongtai.system.model.vo.HiveTableDataVo;
import cn.cnic.zhongtai.system.service.GenTableColumnService;
import cn.cnic.zhongtai.system.service.GenTableService;
import cn.cnic.zhongtai.system.service.HiveService;
import cn.cnic.zhongtai.utils.CommonUtils;
import cn.cnic.zhongtai.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/hive")
@Slf4j
public class HiveController {

    @Resource
    private HiveService hiveService;

    @Resource
    private GenTableColumnService genTableColumnService;
    @Resource
    private GenTableService genTableService;



    @Deprecated
    @GetMapping("/findAllTablesInfo")
    public String findAllTablesInfo(){
        Message retMessage;
        try{
            retMessage = new Message(hiveService.findAllTablesInfo());
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    /**
     * 获取hive库中所有表名
     * @return 表名的集合
     */
    @Deprecated
    @GetMapping("/getAllTableName")
    public String getAllTableName(){

        Message retMessage;
        try {
            retMessage = new Message(hiveService.getAllTableName());
        }catch (Exception e) {
            log.error(e.getLocalizedMessage());
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();

    }

    /**
     * 根据表名获取所有字段名
     * @param tableName
     * @return columns
     */
    @Deprecated
    @GetMapping("/getColumnsByTableName/{tableName}")
    public String getColumnsByTableName(@PathVariable("tableName")String tableName){
        Message retMessage;
        try{
            retMessage = new Message(hiveService.getColumnsByTableName(tableName));
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @DeleteMapping("/delete")
    public String deleteByNames(String[] names){
        Message retMessage;
        try{
            hiveService.deleteByNames(names);
            retMessage = new Message("删除成功");
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @DeleteMapping("/deleteAll")
    public String deleteAll(){
        Message retMessage;
        try{
            hiveService.deleteAll();
            retMessage = new Message("删除成功");
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }



    /**
     * 检查表是否在hive中存在
     * @param tableName
     * @return columns
     */
    @GetMapping("/checkTableInHive")
    public String checkTableInHive(String tableName){
        Message retMessage;
        try{
            retMessage = new Message(hiveService.validateTableNameExistByHive(tableName));
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @RequestMapping(value = "/getHiveContent")
    @ResponseBody
    public String getHiveContent( String tableName){
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        if (StringUtils.isBlank(tableName) || "null".equals(tableName)) {
            log.error("tableName 为空,查询失败！");
            rtnMap.put("msg", "该节点未绑定映射表信息...");
        }else{
            try {
                Long tamp=System.currentTimeMillis();
                log.info(tamp.toString());
                List<Map<String, Object>> list = hiveService.getDateByTableName(tableName,100);

                List<ColumnSimpleInfo> columnList  =  hiveService.getColumnsByTableName(tableName);

                int count = hiveService.getCountByHiveTableName(tableName);

                rtnMap.put("count", count);
                rtnMap.put("code", 200);
                rtnMap.put("data", list);
                rtnMap.put("columnList", columnList);
            }catch (Exception e) {
                log.error("hive 内容查询失败",e);
                rtnMap.put("msg",  "映射"+tableName +"表不存在,查询失败");
            }
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }

    @RequestMapping(value = "/getHiveContentV2")
    @ResponseBody
    public String getHiveContentV2( String tableName){
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        if (StringUtils.isBlank(tableName) || "null".equals(tableName)) {
            log.error("tableName 为空,查询失败！");
            rtnMap.put("msg", "该节点未绑定映射表信息...");
        }else{
            try {
                Long tamp=System.currentTimeMillis();
                log.info(tamp.toString());
                List<String> columnList;
                List<Map<String, Object>> list = hiveService.getDateByTableName(tableName,100);
                if (CollectionUtils.isEmpty(list)) {
                   columnList = hiveService.getColumnsByTableName(tableName)
                           .stream()
                           .map(columnSimpleInfo -> columnSimpleInfo.getColumnName())
                           .collect(Collectors.toList());
                } else {
                    List<String> cols = new ArrayList<>(list.get(0).keySet());
                    columnList = cols.stream().map(col -> col.split("\\.")[1]).collect(Collectors.toList());
                }
                //int count = hiveService.getCountByHiveTableName(tableName);

                //rtnMap.put("count", count);
                rtnMap.put("code", 200);
                rtnMap.put("data", list);
                rtnMap.put("columnList", columnList);
            }catch (Exception e) {
                log.error("hive 内容查询失败",e);
                rtnMap.put("msg",  "映射"+tableName +"表不存在,查询失败");
            }
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }

    @GetMapping("/getDataByTableName")
    public String getDataByTableName(String tableName) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        if (StringUtils.isBlank(tableName) || "null".equals(tableName)) {
            log.error("tableName 为空,查询失败！");
            rtnMap.put("msg", "该节点未绑定映射表信息...");
        }else{
            try {
                HiveTableDataVo hiveTableDataVo = hiveService.getHiveTableInfo(tableName);

            }catch (Exception e) {
                log.error("hive 内容查询失败",e);
                rtnMap.put("msg",  "映射"+tableName +"表不存在,查询失败");
            }
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }

    /**
     * http测试接口,后期会删
     * @param tableId
     * @return
     */
    @RequestMapping(value = "/httpContentTest")
    @ResponseBody
    public String httpContentTest( String tableId){
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        if (StringUtils.isBlank(tableId) || "null".equals(tableId)) {
            rtnMap.put("msg", "tableId 为空,查询失败...");
        }else{
            try {
                GenTable genTable = genTableService.selectByPrimaryKey(Long.valueOf( tableId));
                List<GenTableColumn> list =  genTableColumnService.selectModelColumnByTableId(tableId);
                rtnMap.put("code", 200);
                rtnMap.put("data", list);
                rtnMap.put("name", genTable.getTableName()+"_"+ CommonUtils.getCode(6));
            }catch (Exception e) {
                rtnMap.put("msg",  "数据不存在,查询失败");
            }
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }

}
