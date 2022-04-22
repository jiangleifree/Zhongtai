package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.*;
import cn.cnic.zhongtai.system.service.*;
import cn.cnic.zhongtai.utils.JsonUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.ws.rs.QueryParam;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/topic")
@Slf4j
@Api(value = "主题库", tags = "主题库")
public class TopicController {


    @Resource
    private TopicService topicService;
    @Resource
    private TableInfoService tableInfoService;
    @Resource
    private TopicInterfaceService topicInterfaceService;
    @Resource
    private TopicInterfaceStatisticsService statisticsService;
    @Resource
    private InterfaceParamService interfaceParamService;

    @GetMapping("/findAllTopics")
    @ApiOperation(value = "查询所有主题库信息", response = String.class)
    public String getAllTopic() {

        Map<String, Object> rtnMap = new HashMap<String, Object>();
        try {
            List<TopicRepository> allTopic = topicService.getAllTopic();
            rtnMap.put("data", allTopic);
            rtnMap.put("code", 200);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            rtnMap.put("msg", "error");
            rtnMap.put("code", "500");
        }
        return JsonUtils.toJsonNoException(rtnMap);

    }

    @GetMapping("/getTopicInterfaceByTopic")
    @ApiOperation(value = "获取主题库对外提供服务接口", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "topicName", value = "主题库名称", required = true)
    })
    public String getTopicInterfaceByTopic(String topicName) {
        Message retMessage;
        try {
            List<TopicInterface> interfaces = topicInterfaceService.getAllByTopicName(topicName);
            //Map<String, List<TopicInterface>> retData = interfaces.stream().collect(Collectors.groupingBy(TopicInterface::getTableName));
            retMessage = new Message(interfaces);
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @GetMapping("/retTemplate")
    @ApiOperation(value = "返回json模板", response = String.class)
    public String retTemplate() {
        String json = "{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"or\": [\n" +
                "        {\n" +
                "          \"EQ\": {\n" +
                "            \"parent\": \"李刚\"\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"bool\": {\n" +
                "            \"and\": [\n" +
                "              {\n" +
                "                \"GT\": {\n" +
                "                  \"age\": 18\n" +
                "                }\n" +
                "              },\n" +
                "              {\n" +
                "                \"bool\": {\n" +
                "                  \"or\": [\n" +
                "                    {\n" +
                "                      \"EQ\": {\n" +
                "                        \"primary\": \"新城一小\"\n" +
                "                      }\n" +
                "                    },\n" +
                "                    {\n" +
                "                      \"EQ\": {\n" +
                "                        \"seniorHigh\": \"实验高中\"\n" +
                "                      }\n" +
                "                    }\n" +
                "                  ]\n" +
                "                }\n" +
                "              },\n" +
                "              {\n" +
                "                \"EQ\": {\n" +
                "                  \"gender\": \"男\"\n" +
                "                }\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  },\n" +
                " \"group\":[\"col1\",\"col2\"],\n" +
                " \"order\":[\n" +
                "  {\"col1\":\"desc\"},\n" +
                "  {\"col2\":\"asc\"}\n" +
                " ],\n" +
                " \"size\":10,\n" +
                " \"from\":0,\n" +
                " \"topic\": \"test0209\",\n" +
                " \"table\": \"person\"\n" +
                "}";
        return json;
    }

    @GetMapping("/getAllDataByTableName")
    public String getAllDataByTableName(@QueryParam(value = "topicName") String topicName, @QueryParam(value = "tableName") String tableName) {
        Message retMessage;
        try {
            List<Map<String, Object>> data = topicService.getAllDataByTableName(topicName, tableName);
            retMessage = new Message(data);
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @GetMapping("/getAllFieldsByDBNameAndTableName")
    @ApiOperation(value = "获取表的所有字段", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "topicName", value = "主题库名称", required = true, dataType = "String"),
            @ApiImplicitParam(name = "tableName", value = "表名", required = true, dataType = "String")
    })
    public String getAllFieldsByDBNameAndTableName(String topicName, String tableName) {
        Message retMessage;
        try {
            List<String> fields = topicService.getAllFieldsByDBNameAndTableName(topicName, tableName);
            retMessage = new Message(fields);
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    /**
     * 根据主题库名称查找库下面所有table
     *
     * @param topicName
     * @return
     */
    @GetMapping("/findTablesInfoByTopicName")
    public String findTablesByTopicName(String topicName) {
        Message retMessage;

        try {
            List<TableInfo> tables = tableInfoService.findTablesByTopicName(topicName);
            retMessage = new Message(tables);

        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();

    }

    @GetMapping("/getTableInfoByTopicAndTableName")
    @ApiOperation(value = "获取单表详细信息", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "topicName", value = "主题库名称", required = true, dataType = "String"),
            @ApiImplicitParam(name = "tableName", value = "表名", required = true, dataType = "String")
    })
    public String getTableInfoByTopicAndTableName(String topicName, String tableName) {
        Message retMessage;
        try {

            TableInfo tableInfo = topicService.getTableInfoByTopicAndTableName(topicName, tableName);
            retMessage = new Message(tableInfo);
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @PostMapping("/createTopic")
    @ApiOperation(value = "创建主题库", response = String.class)
    public String createTopic(@RequestBody TopicRepository topicRepository) {

        Message retMessage;
        topicRepository.setCreateTime(new Date());
        try {
            //topicName不能为null
            if (StringUtils.isEmpty(topicRepository.getTopicName())) {
                throw new RuntimeException("TopicName must not be null");
            }
            //先判断主题库是否已经存在
            if (null != topicService.findTopicByTopicName(topicRepository.getTopicName())) {
                throw new RuntimeException("TopicName already exists");
            }
            //创建topic
            topicService.createTopic(topicRepository);
            retMessage = new Message("create topic success");

        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            retMessage = new Message(e);

        }
        return retMessage.toJsonNoException();

    }

    @DeleteMapping("/delete")
    @ApiOperation(value = "删除主题库", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "topicName", value = "主题库名称", required = true, dataType = "String")
    })
    public String deleteTopic(String topicName) {
        try {
            topicService.deleteTopic(topicName);
            return new Message<>("删除成功").toJsonNoException();
        } catch (Exception e) {
            e.printStackTrace();
            return new Message<>(e).toJsonNoException();
        }
    }

    @PostMapping("/authentication")
    @ApiOperation(value = "验证主题库权限", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "topicName", value = "主题库名称", required = true, dataType = "String"),
            @ApiImplicitParam(name = "userName", value = "账号", required = true, dataType = "String"),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String")
    })
    public String authentication(@RequestBody TopicRepository topicRepository) {
        Message retMessage;
        try {

            boolean isPassed = topicService.authentication(topicRepository.getTopicName(), topicRepository.getUserName(), topicRepository.getPassword());
            retMessage = new Message(isPassed);
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @GetMapping("interface/statistics")
    @ApiOperation(value = "接口调用详情", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "interfaceId", value = "接口id", required = true, dataType = "String"),
            @ApiImplicitParam(name = "page", value = "当前页", dataType = "String"),
            @ApiImplicitParam(name = "limit", value = "每页展示数量", dataType = "String")
    })
    public String statistics(@RequestParam(value = "page", defaultValue = "1") String page,
                             @RequestParam(value = "limit", defaultValue = "10") String limit,
                             @RequestParam(value = "interfaceId", defaultValue = "") String interfaceId) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        //当前页
        Integer currPage = Integer.parseInt(page.trim());
        //每页的数量
        Integer pageSize = Integer.parseInt(limit.trim());
        //String interfaceId = topicInterfaceService.getInterfaceIdByUrl(url);
        PageHelper.startPage(currPage, pageSize, true);
        List<TopicInterfaceStatistics> statistics = statisticsService.getListByInterfaceId(interfaceId);
        PageInfo<TopicInterfaceStatistics> pageInfo = new PageInfo<>(statistics);

        try {
            rtnMap.put("msg", "success");
            rtnMap.put("code", "0");
            rtnMap.put("data", statistics);
            rtnMap.put("count", pageInfo.getTotal());
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            rtnMap.put("msg", "error");
            rtnMap.put("code", "222222");
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }

    @GetMapping("/testSql")
    public Message testSql(String topicName, String sql) {
        Message ret;
        try {
            List<Map<String, String>> queryResults = topicService.testSql(topicName, sql);
            ret = new Message(queryResults);
        } catch (Exception e) {
            ret = new Message(e);
        }
        return ret;
    }

    @GetMapping("/getFieldsByTopicName")
    public String getFieldsByTopicName(String topicName) {
        Message mes;
        try {
            mes = new Message(topicService.getAllFieldsByTopicName(topicName));
        } catch (Exception e) {
            mes = new Message(e);
        }
        return mes.toJsonNoException();
    }


    @PostMapping("/interface/create")
    public String createInterface(@RequestBody InterfaceParam param) {
        Message ret;
        try {

            topicService.createInterface(param);
            ret = new Message(200, "创建成功", "", null);
        } catch (Exception e) {
            ret = new Message(e);
        }
        return ret.toJsonNoException();
    }

    @GetMapping("/getTopicInterfaceInfo")
    public Map<String, Object> getTopicInterfaceInfo(String topicName) {

        Map<String, Object> ret = topicService.getTopicInterfaceInfo(topicName);
        return ret;
    }

}
