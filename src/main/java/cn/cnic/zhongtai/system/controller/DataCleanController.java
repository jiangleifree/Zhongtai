package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.ColumnSimpleInfo;
import cn.cnic.zhongtai.system.model.DataMapping;
import cn.cnic.zhongtai.system.model.HiveTable;
import cn.cnic.zhongtai.system.model.vo.DataClearVO;
import cn.cnic.zhongtai.system.service.DataClearService;
import cn.cnic.zhongtai.system.service.DataMappingService;
import cn.cnic.zhongtai.system.service.HiveService;
import cn.cnic.zhongtai.utils.JsonUtils;
import com.alibaba.fastjson.JSONArray;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
*
 * 数据清洗控制类
* Created  by wdd on 2019/11/21
*
**/
@Controller
@RequestMapping(value = "/dataClean")
@Slf4j
@Api(value = "数据清洗", tags = "数据清洗")
public class DataCleanController {


    @Resource
    private HiveService hiveService;


    @Resource
    private DataClearService dataClearService;

    @Resource
    private DataMappingService dataMappingService;




    @RequestMapping(value = "/page" ,method = RequestMethod.GET)
    public ModelAndView index(ModelAndView modelAndView,String tableName){
        if (null == modelAndView) {
            modelAndView = new ModelAndView();
        }
        List<ColumnSimpleInfo> columnList = null;
        modelAndView.setViewName("data_clean");
        if(StringUtils.isNotBlank(tableName)){
            try {
                modelAndView.addObject("data",   hiveService.getDateByTableName(tableName,20));
                int count = hiveService.getCountByHiveTableName(tableName);
                columnList =  hiveService.getColumnsByTableName(tableName);

                //虚拟目录信息
                DataMapping dataMapping =  dataMappingService.selectByTableName(tableName);
                if(null != dataMapping){
                    modelAndView.addObject("dataMappingId",  dataMapping.getId());
                    modelAndView.addObject("dataMappingName",  dataMapping.getName());
                }

                //数据分析信息
                JSONArray str =  hiveService.getColumnStatistics(tableName,columnList);
                if(null != str && !"[]".equals(str.toString())){
                    modelAndView.addObject("columnStatistics",  str.toJSONString());
                }else {
                    //执行数据分析脚本
                    dataClearService.statistical_Information(tableName,columnList);
                }

                modelAndView.addObject("columnList",  columnList);
                modelAndView.addObject("tableName",  tableName);
                //记录数
                modelAndView.addObject("count", count);
                //表字段数量
                modelAndView.addObject("columnCount", columnList.size());
                String arr = "";
                for (ColumnSimpleInfo list:columnList) {
                    arr+= list.getColumnName()+",";
                }
                arr = arr.substring(0,arr.length()-1);
                modelAndView.addObject("columnStr",  arr);
            } catch (Exception e) {
                log.error("hive表查询失败："+e);
                modelAndView.addObject("tableName",  tableName);
                modelAndView.addObject("errorInfo",  tableName+" 表数据加载失败,请稍后再试");
            }
        }
        return modelAndView;

    }


    /**
     * 数据清洗  run job
     * @param dataClearVO
     * @return
     */
    @RequestMapping(value = "/runJob" ,method = RequestMethod.POST)
    @ResponseBody
    public String runJob(@RequestBody DataClearVO dataClearVO){
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        if (null == dataClearVO && StringUtils.isBlank(dataClearVO.getTableName())) {
           log.error("数据清洗提交任务：参数为空,提交错误！");
            rtnMap.put("errMsg", "参数为空,提交错误！");
        }else{
            try {
                Map<String, Object> map = dataClearService.runJobToClearData(dataClearVO);
                rtnMap.put("msg", map.get("msg"));
                rtnMap.put("code", map.get("code"));
                rtnMap.put("taskId", map.get("taskId"));
            }catch (Exception e) {
                log.error("数据清洗提交任务异常",e);
                rtnMap.put("msg", e.getMessage());
            }
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }



    /**
     * hive表查询  此处查的mysql中hive表信息
     *
     * @param request
     * @return
     */
    @RequestMapping("hiveTableListByTableName")
    @ResponseBody
    public List<HiveTable> hiveTableListByTableName(HttpServletRequest request) {
        String tableName = request.getParameter("tableName");
        List<HiveTable> list = hiveService.hiveTableListByTableName(tableName);
        return null != list && !list.isEmpty() ? list : null;
    }


    /**
     * hive 表更新到mysql
     * @return
     */
    @RequestMapping("hiveTableRefreshToMysql")
    @ResponseBody
    public String hiveTableRefreshToMysql() {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        try {
            hiveService.refreshHiveTable();
            rtnMap.put("code", 200);
        }catch (Exception e){
            log.error("hive 表更新到mysql异常...",e);
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }

    /**
     * 分词数据
     * @return
     */
    @RequestMapping("participleeColumn")
    @ResponseBody
    public String participleeColumn(@RequestBody DataClearVO dataClearVO) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        try {
            dataClearService.participleeColumn(dataClearVO);
            rtnMap.put("code", 200);
            rtnMap.put("msg", "任务提交成功");
        }catch (Exception e){
            log.error("任务提交异常",e);
            rtnMap.put("msg", e.getMessage());
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }


}
