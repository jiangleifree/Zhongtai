package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.system.config.Launcher;
import cn.cnic.zhongtai.system.listener.GenerateNeo4jMetaDataListener;
import cn.cnic.zhongtai.system.mapper.InterfaceParamMapper;
import cn.cnic.zhongtai.system.mapper.MapPortMapper;
import cn.cnic.zhongtai.system.mapper.MapStorageMapper;
import cn.cnic.zhongtai.system.mapper.TaskMapper;
import cn.cnic.zhongtai.system.model.*;
import cn.cnic.zhongtai.system.neo4j.Neo4jFactory;
import cn.cnic.zhongtai.system.service.InterfaceParamService;
import cn.cnic.zhongtai.system.service.MapStorageService;
import cn.cnic.zhongtai.utils.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

@Service
@Transactional
@Slf4j
public class MapStorageServiceImpl implements MapStorageService {

    @Value("${syspara.HiveDbName}")
    private String hiveDbName;
    @Resource
    private MapStorageMapper mapStorageMapper;
    @Resource
    private MapPortMapper mapPortMapper;
    @Resource
    private HdfsUtil hdfsUtil;
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private Launcher launcher;

    @Resource
    private InterfaceParamService interfaceParamService;

    @Resource
    private InterfaceParamMapper interfaceParamMapper;

    @Override
    public void create(MapStorage mapStorage) {
        //设置创建时间
        mapStorage.setCreateTime(new Date());
        //设置对应的英文名字
        mapStorage.setMapNameEn("map" + Calendar.getInstance().getTimeInMillis());

        List<MapStorageTable> mapTables = mapStorage.getMapTables();
        Map<String, Object> sparkParam = new HashMap<>();
        JSONArray desc = new JSONArray();
        List<String> nodes = new ArrayList<>();
        List<String> relationShips = new ArrayList<>();
        for (MapStorageTable table : mapTables) {
            Map<String, String> descMap = new HashMap<>();
            descMap.put("type", table.getType());
            descMap.put("tablename", table.getTableName());
            descMap.put("column", table.getColumns());
            descMap.put("hivedbname", hiveDbName + "." + table.getTableName());
            desc.add(descMap);
            if (table.getType().equals("node")) {
                nodes.add(table.getTableName());
            } else if (table.getType().equals("relation")) {
                relationShips.add(table.getTableName());
            }

        }

        //hdfs创建文件夹
        String mapPath = mapStorage.getMapNameEn();
        try {
            hdfsUtil.mkdir("/zhongtai/neo4j_data/" + mapPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //加入参数  hdfs路径
        sparkParam.put("path", mapPath);
        sparkParam.put("desc", desc);

        //sparkTask重定向输出日志
        String logFilePath = "/data/tobaccoZhongtai/neo4j/log/neo4j" + CommonUtils.getUUID32() + ".log";
        File logFile = new File(logFilePath);
        try {
            logFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //创建任务
        int taskId = createTask(StringUtils.join(nodes, ","),
                StringUtils.join(relationShips, ","),
                logFilePath, mapStorage.getMapNameEn(), mapPath);
        //存储图库基本信息
        mapStorageMapper.create(mapStorage);


        Map<String, Object> listenerParam = new HashMap<>();
        listenerParam.put("nodes", StringUtils.join(nodes, ","));
        listenerParam.put("relationShips", StringUtils.join(relationShips, ","));
        listenerParam.put("logFilePath", logFilePath);
        listenerParam.put("mapNameEn", mapStorage.getMapNameEn());
        listenerParam.put("mapPath", mapPath);

        GenerateNeo4jMetaDataListener listener = new GenerateNeo4jMetaDataListener(taskId,
                mapStorage.getMapNameEn(), listenerParam);

        //提交spark任务
        try {
            launcher.CreateSparkLauncher(Constant.SPARK_JAR_NEO4J, JsonUtils.toJsonNoException(sparkParam), listener, new File(logFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 创建task
     */
    public int createTask(String nodes, String relationShips, String logFilePath, String mapNameEn, String mapPath) {
        Map<String, String> jsonParam = new HashMap<>();
        jsonParam.put("nodes", nodes);
        jsonParam.put("relationShips", relationShips);
        jsonParam.put("logFilePath", logFilePath);
        jsonParam.put("mapNameEn", mapNameEn);
        jsonParam.put("mapPath", mapPath);

        Task task = new Task();
        task.setCreateTime(new Date());
        task.setTaskType(Task.Neo4jTask);
        task.setTaskName("Neo4jTask");
        task.setStatus(Task.WAITING);
        task.setJsonData(JSON.toJSONString(jsonParam));
        taskMapper.createTask(task);
        return task.getTaskId();
    }

    @Override
    public List<MapStorage> getAll() {
        return mapStorageMapper.getAll();
    }

    @Override
    public void deleteByMapName(String mapName) {

        String mapNameEn = this.getMapNameEnByMapName(mapName);

        //删除map记录
        mapStorageMapper.deleteByMapName(mapName);
        //删除对应的port记录
        mapPortMapper.deleteByMapNameEn(mapNameEn);
        //rm docker
        this.rmDockerMap(mapNameEn);
        //图库移动到map-deleted目录
        String dbFilePath = "/data/tobaccoZhongtai/neo4j/neo4j-3.5.8/map-docker/" + mapNameEn;
        String deletePath = "/data/tobaccoZhongtai/neo4j/neo4j-3.5.8/map-deleted/";
        try {
            FileUtils.moveDirectoryToDirectory(new File(dbFilePath), new File(deletePath), true);
        } catch (IOException e) {
            log.error("move file failed");
            e.printStackTrace();
        }
        //UnixUtils.run(new String[]{"mv " + dbFilePath + " /data/tobaccoZhongtai/neo4j/neo4j-3.5.8/map-deleted/"}, false);

    }

    @Override
    public void startMap(String mapNameEn) {

        //启动图库
        UnixUtils.run(new String[]{"docker start " + mapNameEn}, false);
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //启动成功后设置启动状态为doing
        mapStorageMapper.changeMapToDoingByMapNameEn(mapNameEn);
    }

    @Override
    public String getStatusByMapName(String mapName) {
        return mapStorageMapper.getStatusByMapName(mapName);
    }

    @Override
    public void stopMap(String mapNameEn) {
        //UnixUtils.run(new String[]{"nohup /data/tobaccoZhongtai/neo4j/neo4j-3.5.8/" + mapName + "/bin/neo4j stop > /dev/null 2>&1 &"}, false);
        UnixUtils.run(new String[]{"docker stop " + mapNameEn}, false);
        //把图库启动状态设置为false
        mapStorageMapper.changeMapToClosedByMapNameEn(mapNameEn);
    }

    @Override
    public void rmDockerMap(String mapNameEn) {
        UnixUtils.run(new String[]{"docker stop " + mapNameEn}, false);
        UnixUtils.run(new String[]{"docker rm " + mapNameEn}, false);
    }

    @Override
    public void changeStatusByMapNameEn(String mapNameEn, String status) {
        mapStorageMapper.changeStatusByMapNameEn(mapNameEn, status);
    }

    @Override
    public void deleteByMapNameEn(String mapNameEn) {
        mapStorageMapper.deleteByMapNameEn(mapNameEn);
    }

    @Override
    public int getTotalCount() {
        return mapStorageMapper.getTotalCount();
    }

    @Override
    public String getMapNameEnByMapName(String mapName) {
        return mapStorageMapper.getMapNameEnByMapName(mapName);
    }

    @Override
    public Map<String, Object> getLabelsByNeo4jUrl(String neo4jUrl) {
        Map<String, Object> map = new HashMap<>();
        //String uri = "bolt://127.0.0.1:7486";
        String username = "";
        String password = "";
        try {
            Session session = Neo4JUtil.getSession(neo4jUrl, username, password);
            List<String> labels = Neo4JUtil.getLabels(session);
            for (String label : labels) {
                Set<String> nodeFields = Neo4JUtil.getNodeFields(session, label);
                map.put(label, nodeFields);
            }
            Neo4JUtil.closeSession(session);
        } catch (Exception e) {
            log.error("neo4j查询label错误：", e);
        }
        return map;
    }

    @Override
    public Map<String, Object> getDataByCql(String neo4jUrl, String cql) {
        //Session session = null;
        //Driver driver = null;
        Map<String, Object> ret = new HashMap<>();
        List<Map<String, Object>> retList = new ArrayList<>();
        Map<String, Object> retMap = new HashMap<>();
        List<Map<String, Object>> nodesList = new ArrayList<>();
        List<Map<String, Object>> relationsList = new ArrayList<>();
        Set<String> category = new HashSet<>();

        try {
            //session = Neo4JUtil.getSession(neo4jUrl, "", "");
            //driver = Neo4JUtil.getDriver(neo4jUrl, "", "");
            Driver driver = Neo4jFactory.getDriverFromPool(neo4jUrl)
                    .orElseGet(new Supplier<Driver>() {
                        @Override
                        public Driver get() {
                            Driver ret = Neo4jFactory.newDriver(neo4jUrl, "", "").get();
                            Neo4jFactory.addDriverToPool(neo4jUrl, ret);
                            return ret;
                        }
                    });

            //StatementResult result = Neo4JUtil.execute(session, cql);
            StatementResult result = Neo4JUtil.execute(driver, cql);

            while (result.hasNext()) {
                Record record = result.next();
                Set<String> keys = new HashMap<>(record.asMap()).keySet();
                try {
                    for (String key : keys) {
                        Path path = record.get(key).asPath();
                        Iterable<Node> nodes = path.nodes();
                        Iterable<Relationship> relationships = path.relationships();
                        for (Node node : nodes) {
                            Map<String, Object> nod = new HashMap<>(node.asMap());
                            for (String label : node.labels()) {
                                nod.put("category", label);
                                category.add(label);
                            }
                            nod.put("id", node.id());
                            nodesList.add(nod);
                        }
                        for (Relationship relationship : relationships) {
                            Map<String, Object> rela = new HashMap<>(relationship.asMap());
                            rela.put("start", relationship.startNodeId());
                            rela.put("end", relationship.endNodeId());
                            rela.put("type", relationship.type());
                            relationsList.add(rela);
                        }
                    }
                } catch (Exception e) { //不能转化为path  直接返回map
                    retList.add(new HashMap<>(record.asMap()));
                }
            }

            retMap.put("nodes", filterListByName(nodesList));
            retMap.put("category", category);
            retMap.put("relations", relationsList);

        } finally {
//            if (driver != null) {
//                Neo4JUtil.closeDriver(driver);
//            }
        }

        ret.put("data", retList);
        ret.put("path", retMap);
        return ret;

    }

    private static List<Map<String, Object>> filterListByName(List<Map<String, Object>> list) {
        if (null == list || list.size() <= 0) {
            return list;
        } else {
            for (int i = 0; i < list.size(); i++) {
                Map<String, Object> map1 = list.get(i);
                for (int j = i + 1; j < list.size(); j++) {
                    if (map1.get("id").equals(list.get(j).get("id"))) {
                        list.remove(j);
                        j--;
                    }
                }
            }
            return list;
        }

    }

    private static List<Map<String, Object>> filterData(List<Map<String, Object>> data) {
        Set<String> keys = new HashSet<>();
        List<Map<String, Object>> retData = new ArrayList<>();
        if (CollectionUtils.isEmpty(data)) {
            return Collections.EMPTY_LIST;
        }
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> var = data.get(i);
            String key = (String) var.get("id");
            if (!keys.contains(key)){
                retData.add(var);
            }
            keys.add(key);
        }
        return retData;
    }


    @Override
    public void createInterface(InterfaceParam param) {
        param.setCreateDate(new Date());
        param.setBelong("mapStorage");
        param.setConsumes("application/json");
        param.setSummary(param.getSummary());
        param.setUrl("/map/api/mapStorageService/" + param.getUrl());

        JSONArray parameters = new JSONArray();
        parameters.addAll(param.getParameterList());
        JSONArray responses = new JSONArray();
        responses.addAll(param.getResponseList());

        param.setParametersJsonStr(parameters.toJSONString());
        param.setResponsesJsonStr(responses.toJSONString());
        interfaceParamMapper.insert(param);
    }

    @Override
    public Map<String, Object> service(HttpServletRequest request) {
        String uri = request.getRequestURI();
        InterfaceParam interfaceParam = interfaceParamService.getByUrl(uri);
        if (null == interfaceParam) {
            throw new RuntimeException("接口查询异常...");
        }
        String cypher = interfaceParam.getSql();

        List<InterfaceParam.Parameter> parameters = JSONArray.parseArray(interfaceParam.getParametersJsonStr(), InterfaceParam.Parameter.class);
        List<InterfaceParam.Parameter> response = JSONArray.parseArray(interfaceParam.getResponsesJsonStr(), InterfaceParam.Parameter.class);
        interfaceParam.setResponseList(response);
        interfaceParam.setParameterList(parameters);

        List<InterfaceParam.Parameter> parameterList = interfaceParam.getParameterList();
        for (int i = 0; i < parameterList.size(); i++) {
            String type = parameterList.get(i).getType();
            String required = parameterList.get(i).getRequired();
            String param = request.getParameter(parameterList.get(i).getName());
            switch (type) {
                case "string":
                    if (StringUtils.isNotBlank(param)) {
                        String val = request.getParameter(parameterList.get(i).getName());
                        //开始替换入参 生成完整正确的cypher
                        cypher = cypher.replaceAll("\\{[" + parameterList.get(i).getName() + "^}]*\\}", "\"" + val + "\"");
                        break;
                    } else if ("true".equals(required)) {
                        throw new RuntimeException("请输入必填项参数...");
                    }

                case "int":
                    if (StringUtils.isNotBlank(param)) {
                        int num = Integer.parseInt(request.getParameter(parameterList.get(i).getName()));
                        //开始替换入参 生成完整正确的cypher
                        String name = parameterList.get(i).getName();
                        cypher = cypher.replaceAll("\\{[" + name + "^}]*\\}", num + "");
                        break;
                    } else if ("true".equals(required)) {
                        throw new RuntimeException("请输入必填项参数...");
                    }
            }
        }
        log.info(cypher);
        Map<String, Object> result = getDataByCql(interfaceParam.getMapStorageUrl(), cypher);
        return result;

    }

    @Override
    public Map<String, Object> getTopicInterfaceInfo(String mapName) {

        MapStorage mapStorage = mapStorageMapper.getMapStorageByName(mapName);
        List<InterfaceParam> interfaceParams = interfaceParamService.getListByTopicName(mapName);
        int interfaceCount = interfaceParams.size();

        Map<String, Object> ret = new HashMap<>(2);
        Map<String, Object> info = new HashMap<>(4);
        info.put("mapName", mapName);
        info.put("createTime", DateUtils.dateTimeToStr(mapStorage.getCreateTime()));
        info.put("comment", mapStorage.getMapComment());
        //图库状态
        info.put("status", mapStorage.getStatus());
        //图库地址
        info.put("mapUrl", mapStorage.getMapUrl());
        info.put("interfaceCount", interfaceCount);
        ret.put("info", info);

        List<Map<String, Object>> paths = new ArrayList<>();
        Map<String, Object> path = null;
        for (int i = 0; i < interfaceParams.size(); i++) {
            path = new HashMap<>();
            InterfaceParam param = interfaceParams.get(i);
            List<InterfaceParam.Parameter> parameters = JSONArray.parseArray(param.getParametersJsonStr(), InterfaceParam.Parameter.class);
            List<InterfaceParam.Parameter> responses = JSONArray.parseArray(param.getResponsesJsonStr(), InterfaceParam.Parameter.class);
            path.put("url", param.getUrl());
            path.put("type", param.getType());
            path.put("consumes", param.getConsumes());
            path.put("produces", param.getProduces());
            path.put("summary", param.getSummary());
            path.put("parameters", parameters);
            path.put("responses", responses);
            paths.add(path);
        }
        ret.put("paths", paths);

        return ret;
    }

    @Override
    public MapStorage getByMapName(String mapName) {
        return mapStorageMapper.getMapStorageByName(mapName);
    }

}
