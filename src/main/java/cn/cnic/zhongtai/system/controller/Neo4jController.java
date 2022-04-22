package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.system.entity.node.NodeGraph;
import cn.cnic.zhongtai.system.model.vo.EchartsRelationVo;
import cn.cnic.zhongtai.system.service.GraphService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("neo4j")
public class Neo4jController {

    @Resource
    private GraphService graphService;

    @GetMapping("/add/node")
    public String addNode(String tableId, String tableName){
        try{
            NodeGraph nodeGraph = NodeGraph.builder()
                    .table_name(tableName)
                    .table_id(tableId)
                    .build();
            graphService.createNode(nodeGraph);
            return "success";
        } catch (Exception e){
            return "false";
        }
    }

    @GetMapping("/add/relation")
    public String addRelation(String startId, String endId){
        try{

            graphService.createRelation(startId, endId);
            return "success";
        } catch (Exception e){
            return "false";
        }
    }

    @GetMapping("/get/relation")
    public EchartsRelationVo getRelation(String tableName){
        return graphService.getSource(tableName);
    }
}
