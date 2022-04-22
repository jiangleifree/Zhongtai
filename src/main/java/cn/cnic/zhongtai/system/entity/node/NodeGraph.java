package cn.cnic.zhongtai.system.entity.node;

import lombok.Builder;
import lombok.Data;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;


@Data
@Builder
@NodeEntity(label = "node")
public class NodeGraph {

    /**
     * id
     */
    @Id
    @GeneratedValue
    private long id;

    /**
     * 展示的名字
     */
    @Property
    private String table_name;
    /**
     * 对应的虚拟表的id
     */
    @Property
    private String table_id;
    /**
     * 来源类型 RootNode才会有
     * xml json csv  jdbc
     */
    @Property
    private String source_type;

    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("{node_id: '")
                .append(this.getId())
                .append("', table_name: '")
                .append(this.getTable_name())
                .append("', table_id: '")
                .append(this.getTable_id())
                .append("', sourceType: '")
                .append(this.getSource_type())
                .append("'}");
        return str.toString();
    }


}
