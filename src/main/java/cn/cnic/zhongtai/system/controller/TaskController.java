package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.Task;
import cn.cnic.zhongtai.system.service.TaskService;
import cn.cnic.zhongtai.utils.JsonUtils;
import cn.cnic.zhongtai.utils.SparkUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/task")
@Slf4j
@Api(tags = "任务控制")
public class TaskController {

    @Resource
    private TaskService taskService;

    @GetMapping("/getAllTask")
    @ApiOperation(value = "获取所有任务", response = String.class)
    public String getAllTask(@RequestParam(value = "page", defaultValue = "1") String page, @RequestParam(value = "limit", defaultValue = "10") String limit) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        //当前页
        Integer currPage = Integer.parseInt(page.trim());
        //每页的数量
        Integer pageSize = Integer.parseInt(limit.trim());
        PageHelper.startPage(currPage, pageSize, true);
        List<Task> tasks = taskService.getAllTask();
        PageInfo<Task> pageInfo = new PageInfo<>(tasks);

        try{
            rtnMap.put("msg", "success");
            rtnMap.put("code", "0");
            rtnMap.put("data", tasks);
            rtnMap.put("count", pageInfo.getTotal());
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            rtnMap.put("msg", "error");
            rtnMap.put("code", "222222");
        }

        return JsonUtils.toJsonNoException(rtnMap);
    }

    @PostMapping("/queryTaskStatuById")
    @ApiOperation(value = "获取spark任务状态")
    public String queryTaskStatuById(String taskId) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);

        if(StringUtils.isNotBlank(taskId)){
            try{
                Task task =  taskService.getTaskStatusById(Integer.parseInt(taskId));
                rtnMap.put("task", task);
                rtnMap.put("code", 200);
            }catch (Exception e){
                log.error(taskId+":获取任务信息异常"+e);
                rtnMap.put("errMsg","获取任务信息异常");
            }
        }else{
            rtnMap.put("errMsg","taskId为空");
        }
        return JsonUtils.toJsonNoException(rtnMap);

    }


    /**
     * Get the address of the log of the flow
     *
     * @param request
     * @return
     */
    @RequestMapping("/getSparkLogByAppId")
    @ResponseBody
    public Map<String, Object> getSparkLogByAppId(HttpServletRequest request) {
        Map<String, Object> rtnMap = new HashMap<>();
        rtnMap.put("code", 500);
        String appId = request.getParameter("applicationId");
        if (StringUtils.isNotBlank(appId)) {
            String amContainerLogs = taskService.getSparkLogByAppId(appId);
            if (StringUtils.isNotBlank(amContainerLogs)) {
                rtnMap.put("code", 200);
                rtnMap.put("stdoutLog", amContainerLogs + "/stdout/?start=0");
                rtnMap.put("stderrLog", amContainerLogs + "/stderr/?start=0");
            } else {
                rtnMap.put("code", 200);
                rtnMap.put("stdoutLog", "Interface call failed");
                rtnMap.put("stderrLog", "Interface call failed");
            }
        } else {
            log.error("appId is null");
        }
        return rtnMap;
    }


    /**
     * Climb to the log by the address of the flow log
     *
     * @param request
     * @param model
     * @return
     */
    @RequestMapping("/getLog")
    @ResponseBody
    public String getLog(HttpServletRequest request, Model model) {
        String rtnMsg = "";
        String urlStr = request.getParameter("url");
        if (StringUtils.isNotBlank(urlStr)) {
            if ("Interface call failed".equals(urlStr)) {
                rtnMsg = "Interface call failed";
            } else {
                rtnMsg = SparkUtils.getHtml(urlStr);
            }
        } else {
            log.warn("urlStr is null");
        }

        return rtnMsg;
    }

}
