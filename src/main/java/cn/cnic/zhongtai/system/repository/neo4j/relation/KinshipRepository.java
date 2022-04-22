package cn.cnic.zhongtai.system.repository.neo4j.relation;

import cn.cnic.zhongtai.system.entity.node.NodeGraph;
import cn.cnic.zhongtai.system.entity.relation.KinshipRelation;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public interface KinshipRepository extends Neo4jRepository<KinshipRelation, Long> {
    @Query("MATCH path =(parent:node)-[r:kinship*1..100]->(child:node) where child.table_id = {tableId} return path")
    List<Map<String, Path>> getSourceRelations(@Param("tableId") String tableId);

    @Query("match (start:node)-[r:kinship*1..100]->(end:node) where end.table_id = {tableId} return start.table_id, start.table_name, end.table_id, end.table_name")
    List<Map<String, Object>> getSource(@Param("tableId") String tableId);

    @Query("match (start:node)-[r:kinship*1..100]->(end:node) where end.table_id = {tableId} return start, end")
    List<NodeGraph> getSource2(@Param("tableId") String tableId);

    @Query("match (start:node), (end:node) where start.table_id = {startId} and end.table_id = {endId} create " +
            "(start)-[r:kinship]->(end)" )
    void save(@Param("startId") String startId, @Param("endId") String endId);

    @Query("match (start:node), (end:node) where start.table_name = {sourceTableName} and end.table_name = {targetTableName} create " +
            "(start)-[r:kinship]->(end)" )
    void saveByTableName(String sourceTableName, String targetTableName);
}
