package cn.cnic.zhongtai.system.repository.neo4j.node;

import cn.cnic.zhongtai.system.entity.node.NodeGraph;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

@Component
public interface NodeGraphRepository extends Neo4jRepository<NodeGraph, Long> {

    @Query("merge (n:node { table_name : {tableName}}) on create set n.table_id = {tableId}")
    void save(@Param("tableId") String tableId, @Param("tableName") String tableName);
}
