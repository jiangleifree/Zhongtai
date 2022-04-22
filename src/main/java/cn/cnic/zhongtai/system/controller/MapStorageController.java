package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.model.InterfaceParam;
import cn.cnic.zhongtai.system.model.MapStorage;
import cn.cnic.zhongtai.system.model.Message;
import cn.cnic.zhongtai.system.service.MapStorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/map")
@Api(tags = "图库")
@Slf4j
public class MapStorageController {
    @Resource
    private MapStorageService mapStorageService;

    @PostMapping("/create")
    @ApiOperation(value = "图库创建")
    public String create(@RequestBody MapStorage mapStorage) {
        Message message;
        try {
            if (StringUtils.isEmpty(mapStorage.getMapName())) {
                throw new RuntimeException("mapName must not be empty");
            }
            MapStorage temp = mapStorageService.getByMapName(mapStorage.getMapName());
            if (temp != null) {
                throw new RuntimeException("mapName 重复");
            }
            mapStorageService.create(mapStorage);
            message = new Message("create MapStorage success");
        } catch (Exception e) {
            message = new Message(e);
        }
        return message.toJsonNoException();
    }

    @GetMapping("/getAll")
    @ApiOperation(value = "获取所有图库基本信息")
    public String getAll() {
        Message message;
        try {
            List<MapStorage> mapStorageList = mapStorageService.getAll();
            message = new Message(mapStorageList);
        } catch (Exception e) {
            message = new Message(e);
        }
        return message.toJsonNoException();
    }

    @GetMapping("/startMap")
    @ApiOperation(value = "启动主题库操作")
    public String startMap(String mapName) {
        Message retMessage;
        try {
            //先判断该图库是否已经启动
            String isStart = mapStorageService.getStatusByMapName(mapName);
            if (isStart.equals(MapStorage.NOT_INIT)) { //图库未初始化
                throw new RuntimeException("图库正在初始化中, 请稍后");
            }
            if (isStart.equals(MapStorage.CLOSED)) { //如果没有启动
                //切换图库
                String mapNameEn = mapStorageService.getMapNameEnByMapName(mapName);
                if (StringUtils.isNotEmpty(mapNameEn)){
                    mapStorageService.startMap(mapNameEn);
                }
            }
            retMessage = new Message("切换成功");
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @GetMapping("/stopAllMap")
    @ApiOperation(value = "关闭所有主题库")
    public String stopAllMap() {
        Message retMessage;
        try {
            //mapStorageService.stopMap();
            retMessage = new Message("关闭成功");
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @GetMapping("/stopMap")
    @ApiOperation(value = "关闭该主题库")
    public String stopMap(String mapName) {
        Message retMessage;
        try {
            //切换图库
            String mapNameEn = mapStorageService.getMapNameEnByMapName(mapName);
            if (StringUtils.isNotEmpty(mapNameEn)){
                mapStorageService.stopMap(mapNameEn);
            }
            retMessage = new Message("关闭成功");
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @DeleteMapping("/delete")
    @ApiOperation(value = "删除主题库")
    public String deleteByMapName(String mapName) {
        Message retMessage;
        try {
            mapStorageService.deleteByMapName(mapName);
            retMessage = new Message("delete success");
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @GetMapping("/getLabelsByNeo4jUrl")
    @ApiOperation(value = "根据图库地址获取图库中所有label", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "neo4jUrl", value = "图库地址", required = true)
    })
    public String getLabelsByNeo4jUrl(String neo4jUrl) {
        Message retMessage;
        try {
            Map<String, Object> map =   mapStorageService.getLabelsByNeo4jUrl(neo4jUrl);
            retMessage = new Message(map);
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }

    @GetMapping("/getDataByCql")
    @ApiOperation(value = "根据图库地址获取图库中所有label", response = String.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "neo4jUrl", value = "图库地址", required = true),
            @ApiImplicitParam(name = "cql", value = "neo4j语法", required = true)
    })
    public String getDataByCql(String neo4jUrl,String cql) {
        Message retMessage;
        try {
            Map<String, Object> data = mapStorageService.getDataByCql(neo4jUrl,cql);
            retMessage = new Message(data);
        } catch (Exception e) {
            retMessage = new Message(e);
        }
        return retMessage.toJsonNoException();
    }


    @PostMapping("/interface/create")
    public String createInterface( @RequestBody  InterfaceParam param) {
        Message ret;
        try {
            mapStorageService.createInterface(param);
            ret = new Message(200,"创建成功", "", null);
        } catch (Exception e) {
            ret = new Message(e);
        }
        return ret.toJsonNoException();
    }

    @RequestMapping("/api/mapStorageService/{mapStorageName}")
    public Message service(@PathVariable String mapStorageName,   HttpServletRequest request) {
        Message mes;
        try {
            Map<String, Object> ret = mapStorageService.service( request);
            mes = new Message(ret);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            mes = new Message(e);
        }
        return mes;

    }

    @GetMapping("/getTopicInterfaceInfo")
    public Map<String, Object> getTopicInterfaceInfo(String mapName) {
        Map<String, Object> ret = mapStorageService.getTopicInterfaceInfo(mapName);
        return ret;
    }

}