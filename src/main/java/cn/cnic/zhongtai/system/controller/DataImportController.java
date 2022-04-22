package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.GenTable;
import cn.cnic.zhongtai.system.model.Message;
import cn.cnic.zhongtai.system.model.vo.DataAddToFtpVo;
import cn.cnic.zhongtai.system.model.vo.DataImportJDBCVo;
import cn.cnic.zhongtai.system.model.vo.DataManualFileImportVo;
import cn.cnic.zhongtai.system.model.vo.HttptoDataVo;
import cn.cnic.zhongtai.system.service.DataImportService;
import cn.cnic.zhongtai.system.service.GenTableService;
import cn.cnic.zhongtai.utils.CommonUtils;
import cn.cnic.zhongtai.utils.JsonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据接入控制层
 * Created  by wdd on 2019/10/22
 **/
@RestController
@RequestMapping(value = "/dataImport")
@Slf4j
@Api(value = "数据导入管理", tags = "在线收割和手工导入")
public class DataImportController {

    @Resource
    private GenTableService genTableService;
    @Resource
    private DataImportService dataImportService;
    /**
     * 数据接入模型选择匹配
     *
     * @param request
     * @return
     */
    @RequestMapping("modelListByTableName")
    public List<GenTable> modelListByTableName(HttpServletRequest request) {
        String tableName = request.getParameter("tableName");
        List<GenTable> list = genTableService.modelListByTableName(tableName);
        return null != list && !list.isEmpty() ? list : null;
    }

    /**
     * neo4j对接hive
     * @return
     */
    @RequestMapping(value = "/neo4jToHive", method = RequestMethod.POST)
    @ApiOperation(value = "在线收割数据neo4jToHive", response = String.class)
    public String neo4jToHive(@RequestBody DataImportJDBCVo dataImportJDBCVo){
        Message retMessage;
        try {
            dataImportService.neo4jToHiveNew(dataImportJDBCVo);
            retMessage = new Message("加入任务队列");

        }catch (Exception e){
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    /**
     * neo4j对接hive
     * @param boltUrl
     * @param userName
     * @param password
     * @return
     */
    @RequestMapping("/neo4jToHiveTestConnect")
    @ApiOperation(value = "neo4jToHiveTestConnect", response = String.class)
    public String neo4jToHiveTestConnect(String boltUrl,String userName,String password){
        Message retMessage;
        try {
            dataImportService.neo4jToHiveTest(boltUrl, userName, password);
            retMessage = new Message("连接成功");

        }catch (Exception e){
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }


    /**
     * 现在手工导入json或者csv文件导入
     *
     * @param dataManualFileImportVo
     * @return
     */
    @RequestMapping(value = "/importCsvOrJsonFile", method = RequestMethod.POST, produces = "text/html; charset=UTF-8")
    @ApiOperation(value = "现在手工导入", notes = "上传本地文件到hdfs进行导入数据", response = String.class)
    public String upload(DataManualFileImportVo dataManualFileImportVo) {
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        //校验参数是否有空
        if (CommonUtils.isObjectFieldNoEmpty(dataManualFileImportVo)) {
            rtnMap.put("msg", "参数错误,请检查后是否都已填写");
        }else {
            try {
                Map<String, Object> map = dataImportService.setDataFile(dataManualFileImportVo);
                rtnMap.put("msg", map.get("msg"));
                rtnMap.put("code", map.get("code"));
            } catch (Exception e) {
                log.error("手工导入json或者csv文件导入异常", e);
                rtnMap.put("msg", e.getMessage());
                rtnMap.put("code", 500);
            }
        }


        return JsonUtils.toJsonNoException(rtnMap);
    }


    /**
     * 在线收割数据
     *
     * @param dataAddToFtpVo
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/data_add_ftp", method = RequestMethod.POST)
    @ApiOperation(value = "在线收割数据", notes = "在线收割数据,根据服务器上的文件进行导入数据", response = String.class)
    public String genTable_add(DataAddToFtpVo dataAddToFtpVo){
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code", 500);
        //校验参数是否有空
        if (CommonUtils.isObjectFieldNoEmpty(dataAddToFtpVo)) {
            rtnMap.put("msg", "参数为空,收割错误");
        } else {
            try {
                Map<String, Object> map = dataImportService.setDataByFtp(dataAddToFtpVo);
                rtnMap.put("msg", map.get("msg"));
                rtnMap.put("code", map.get("code"));
            } catch (Exception e) {
                log.error("在线收割错误", e);
                rtnMap.put("msg", e.getMessage());
            }
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }

    /**
     * 在线收割mysql
     *
     * @param dataImportJDBCVo
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/data_import_jdbc", method = RequestMethod.POST)
    @ApiOperation(value = "在线收割数据jdbc", response = String.class)
    public String dataImportJDBC(@RequestBody DataImportJDBCVo dataImportJDBCVo) {

        Message retMessage;
        try {
            dataImportService.dataImportJDBCNew(dataImportJDBCVo.getJdbcUrl().trim(), dataImportJDBCVo.getUserName(),
                    dataImportJDBCVo.getPassword(), dataImportJDBCVo.getTables() , dataImportJDBCVo.getModelTypeName(),
                    dataImportJDBCVo.getPid()
                );
            retMessage = new Message("已经加入任务列表");
        }catch (Exception e){
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }


    /**
     * 在线收割mysql
     *
     * @param
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/getAllTables", method = RequestMethod.POST)
    @ApiOperation(value = "获取该数据库下的所有tableName", response = String.class)
    public String getAllTables(String jdbcUrl, String userName, String password) {

        Message retMessage;
        try{
            retMessage = new Message(dataImportService.getAllTables(userName, password, jdbcUrl.trim()));
        } catch (Exception e){
            retMessage = new Message(e);
        }

        return retMessage.toJsonNoException();
    }

    @RequestMapping(value = "/getLabelsAndRelations", method = RequestMethod.POST)
    @ApiOperation(value = "获取图库下的所有label和relation", response = String.class)
    public String getLabelsAndRelations(String boltUrl, String userName, String password){
        Message retMessage;
        try{
            retMessage = new Message(dataImportService.getLabelsAndRelations(boltUrl, userName, password));
        } catch (Exception e){
            retMessage = new Message(e);
        }

        return retMessage.toJsonNoException();
    }


    /**
     * http方式接入
     *
     * @param httptoDataVo
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/httpToData", method = RequestMethod.POST)
    @ApiOperation(value = "http方式数据接入", notes = "在线收割,http方式数据接入", response = String.class)
    public String httpToData(@RequestBody HttptoDataVo httptoDataVo){
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        rtnMap.put("code",500);
        if (StringUtils.isBlank(httptoDataVo.getUrl()) || StringUtils.isBlank(httptoDataVo.getType()) || StringUtils.isBlank(httptoDataVo.getCharset())|| StringUtils.isBlank(httptoDataVo.getOperationMode())) {
            rtnMap.put("msg", "请求参数有空值,接入失败");
        }else {
            try {
                Map<String, Object> map = dataImportService.httpToData(httptoDataVo);
                rtnMap.put("data", map.get("data"));//接口返回结果
                rtnMap.put("msg", map.get("msg"));
                rtnMap.put("code", map.get("code"));
            } catch (Exception e) {
                log.error("在线收割错误", e);
                rtnMap.put("msg", e.getMessage());
            }
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }






}
