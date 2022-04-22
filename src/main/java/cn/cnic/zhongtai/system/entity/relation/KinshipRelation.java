package cn.cnic.zhongtai.system.entity.relation;

import cn.cnic.zhongtai.system.entity.node.NodeGraph;
import lombok.Builder;
import lombok.Data;
import org.neo4j.ogm.annotation.*;


@Data
@Builder
@RelationshipEntity(type = "Kinship")
public class KinshipRelation {

    @Id
    @GeneratedValue
    private Long id;
    @StartNode
    private NodeGraph parent;
    @EndNode
    private NodeGraph child;

}
