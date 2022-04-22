package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.system.entity.node.NodeGraph;
import cn.cnic.zhongtai.system.model.vo.EchartsRelationVo;
import cn.cnic.zhongtai.system.repository.neo4j.node.NodeGraphRepository;
import cn.cnic.zhongtai.system.repository.neo4j.relation.KinshipRepository;
import cn.cnic.zhongtai.system.service.GraphService;
import cn.cnic.zhongtai.utils.Neo4JUtil;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GraphServiceImpl implements GraphService {

    @Resource
    private NodeGraphRepository nodeGraphRepository;
    @Resource
    private KinshipRepository kinshipRepository;

    @Override
    public void createNode(NodeGraph nodeGraph) {
        nodeGraphRepository.save(nodeGraph.getTable_id(), nodeGraph.getTable_name());
    }

    @Override
    public void createRelation(NodeGraph parent, NodeGraph child) {

        kinshipRepository.save(parent.getTable_id(), child.getTable_id());
    }

    @Override
    public void createRelation(String startId, String endId) {
        kinshipRepository.save(startId, endId);
    }

    @Override
    public void createRelationByTableName(String sourceTableName, String targetTableName) {
        kinshipRepository.saveByTableName(sourceTableName, targetTableName);
    }

    @Override
    public List<Map<String, Path>> getSourceRelations(String tableId) {
        return kinshipRepository.getSourceRelations(tableId);
    }

    @Override
    public EchartsRelationVo getSource(String tableName) {

        return findRelation(tableName);

    }


    public void createNodeByCql(NodeGraph nodeGraph) {
        Session session = Neo4JUtil.getSession(Constant.NEO4J_URI_7472, "neo4j", "neo4j");
        StringBuilder cql = new StringBuilder();
        cql.append("create (n:node ");
        cql.append(nodeGraph.toString());
        cql.append(") return n");
        Neo4JUtil.execute(session, cql.toString());
        Neo4JUtil.closeSession(session);
    }

    public void createRelationByCql(NodeGraph parent, NodeGraph child) {
        Session session = Neo4JUtil.getSession(Constant.NEO4J_URI_7472, "neo4j", "neo4j");
        StringBuilder cql = new StringBuilder();
        cql.append("match (start:node), (end:node) where start.node_id = '")
                .append(parent.getId())
                .append("' and end.node_id = '")
                .append(child.getId())
                .append("' create (start)-[r:kinship]->(end) return r");
        Neo4JUtil.execute(session, cql.toString());
        Neo4JUtil.closeSession(session);

    }

    public EchartsRelationVo findRelation(String tableName) {

        EchartsRelationVo retVo = new EchartsRelationVo();
        List<EchartsRelationVo.Link> links = new ArrayList<>();
        List<EchartsRelationVo.Node> data = new ArrayList<>();

        List<EchartsRelationVo.Category> categories = new ArrayList<>();
        categories.add(EchartsRelationVo.Category.builder().name("source").build());
        categories.add(EchartsRelationVo.Category.builder().name("target").build());

        Session session = Neo4JUtil.getSession(Constant.NEO4J_URI_7472, "neo4j", "neo4j");
        StringBuilder cql = new StringBuilder();
        cql.append("MATCH path =(parent:node)-[r:kinship*1..100]->(child:node) where child.table_name = '")
                .append(tableName)
                .append("' return path");
        StatementResult result = Neo4JUtil.execute(session, cql.toString());

        while (result.hasNext()) {
            Record record = result.next();
            Path path = record.get("path").asPath();
            Iterator<Relationship> iterator_relation = path.relationships().iterator();
            Iterator<Node> iterator_node = path.nodes().iterator();

            while (iterator_relation.hasNext()) {
                Relationship next = iterator_relation.next();
                long startNodeId = next.startNodeId();
                long endNodeId = next.endNodeId();
                EchartsRelationVo.Link link = EchartsRelationVo.Link.builder()
                        .source(String.valueOf(startNodeId))
                        .target(String.valueOf(endNodeId))
                        .label(EchartsRelationVo.Label.builder().normal(EchartsRelationVo.Normal.builder().formatter("<--").show(true).build()).build())
                        .build();
                links.add(link);
            }
            while (iterator_node.hasNext()) {
                Node next = iterator_node.next();
                Map<String, Object> attr = next.asMap();
                long nodeId = next.id();
                if (tableName.equals(attr.get("table_name"))) {
                    EchartsRelationVo.Node node = EchartsRelationVo.Node.builder()
                            .name(String.valueOf(nodeId))
                            .category("target")
                            .label(EchartsRelationVo.Label.builder().normal(EchartsRelationVo.Normal.builder().formatter((String) attr.get("table_name")).show(true).build()).build())
                            .build();
                    data.add(node);
                } else {
                    EchartsRelationVo.Node node = EchartsRelationVo.Node.builder()
                            .name(String.valueOf(nodeId))
                            .category("source")
                            .label(EchartsRelationVo.Label.builder().normal(EchartsRelationVo.Normal.builder().formatter((String) attr.get("table_name")).show(true).build()).build())
                            .build();
                    data.add(node);
                }

            }

        }
        Neo4JUtil.closeSession(session);


        List<EchartsRelationVo.Node> dataNoPeat = data.stream().distinct().collect(Collectors.toList());
        retVo.setCategories(categories);
        retVo.setData(dataNoPeat);
        retVo.setLinks(links);


        return retVo;
    }

}
