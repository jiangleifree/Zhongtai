package cn.cnic.zhongtai.system.service;

import cn.cnic.zhongtai.system.entity.node.NodeGraph;
import cn.cnic.zhongtai.system.model.vo.EchartsRelationVo;
import org.neo4j.driver.v1.types.Path;

import java.util.List;
import java.util.Map;

public interface GraphService {

    void createNode(NodeGraph nodeGraph);

    void createRelation(NodeGraph parent, NodeGraph child);

    void createRelation(String startId, String endId);

    void createRelationByTableName(String sourceTableName, String targetTableName);

    List<Map<String, Path>> getSourceRelations(String tableId);

    EchartsRelationVo getSource(String tableName);


}
