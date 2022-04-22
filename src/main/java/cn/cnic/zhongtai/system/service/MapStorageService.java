package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.model.InterfaceParam;
import cn.cnic.zhongtai.system.model.MapStorage;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface MapStorageService {

    void create(MapStorage mapStorage);

    List<MapStorage> getAll();

    void deleteByMapName(String mapName);

    void startMap(String mapNameEn);

    String getStatusByMapName(String mapName);

    void stopMap(String mapNameEn);

    void rmDockerMap(String mapNameEn);

    void changeStatusByMapNameEn(String mapNameEn, String status);

    void deleteByMapNameEn(String mapNameEn);

    int getTotalCount();

    String getMapNameEnByMapName(String mapName);

    Map<String,Object> getLabelsByNeo4jUrl(String neo4jUrl);

    Map<String, Object> getDataByCql(String neo4jUrl,String cql);

    void createInterface(InterfaceParam param);

    Map<String, Object> service(HttpServletRequest request);

    Map<String, Object> getTopicInterfaceInfo(String topicName);

    MapStorage getByMapName(String mapName);
}
