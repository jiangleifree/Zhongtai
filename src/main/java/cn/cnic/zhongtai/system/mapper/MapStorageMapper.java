package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.MapStorage;

import java.util.List;

public interface MapStorageMapper {
    void create(MapStorage mapStorage);

    List<MapStorage> getAll();

    void deleteByMapName(String mapName);

    String getStatusByMapName(String mapName);

    void changeAllMapToClosed();

    void changeMapToDoing(String mapName);

    int getTotalCount();

    void changeMapToClosed(String mapName);

    String getMapNameEnByMapName(String mapName);

    void changeMapToDoingByMapNameEn(String mapNameEn);

    void changeMapToClosedByMapNameEn(String mapNameEn);

    void changeStatusByMapNameEn(String mapNameEn, String status);

    void deleteByMapNameEn(String mapNameEn);

    MapStorage getMapStorageByName(String mapName);
}
