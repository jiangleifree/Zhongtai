package cn.cnic.zhongtai.utils;

import cn.cnic.zhongtai.system.model.CqlQueryBody;
import cn.cnic.zhongtai.system.model.Neo4jTypeProperty;
import cn.cnic.zhongtai.system.model.RelationShip;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.v1.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class Neo4JUtil {

    private static Config getConfig() {
        return Config.build()
                .withEncryption()
                .withConnectionTimeout(120, TimeUnit.SECONDS)
                .withMaxConnectionLifetime(60, TimeUnit.MINUTES)
                .withMaxConnectionPoolSize(2000)
                .withConnectionAcquisitionTimeout(20, TimeUnit.SECONDS)
                .toConfig();
    }

    /**
     * get session
     *
     * @param uri
     * @param username
     * @param password
     * @return
     */
    public static Session getSession(String uri, String username, String password) {
        Config config = getConfig();
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password), config).session();
    }

    /**
     * getDriver
     * @param uri
     * @param userName
     * @param password
     * @return
     */
    public static Driver getDriver(String uri, String userName, String password) {
        Config config = getConfig();
        return GraphDatabase.driver(uri, AuthTokens.basic(userName, password), config);
    }

    /**
     * 获取labels
     *
     * @param session
     * @return
     */
    public static List<String> getLabels(Session session) {
        String cql = "call db.labels";
        List<String> labels = new ArrayList<>();
        StatementResult result = execute(session, cql);
        while (result.hasNext()) {
            String label = result.next().get("label").asString();
            labels.add(label);
        }
        return labels;
    }

    /**
     * 获取relationshipTypes
     *
     * @param session
     * @return
     */
    public static List<String> getRelationshipTypes(Session session) {
        String cql = "call db.relationshipTypes";
        List<String> relationshipTypes = new ArrayList<>();
        StatementResult result = execute(session, cql);
        while (result.hasNext()) {
            String relationShipType = result.next().get("relationshipType").asString();
            relationshipTypes.add(relationShipType);
        }
        return relationshipTypes;
    }

    /**
     * 查询该node数据条数
     * @param session
     * @param label
     * @return
     */
    public static int getCountByLabel(Session session, String label) {
        String cql = "match (n:`" + label + "`) return count(n)";
        int count = -1;
        StatementResult result = execute(session, cql);
        while (result.hasNext()) {
            count = result.next().get("count(n)").asInt();
        }
        return count;

    }

    /**
     * 查询以{relationShipType}的start和end
     * @param session
     * @param relationShipType
     * @return
     */
    public static List<RelationShip> getRelationShips(Session session, String relationShipType) {
        StringBuilder sb = new StringBuilder();
        List<RelationShip> relationShips = new ArrayList<>();
        sb.append("match")
                .append(" (start)-[r:`")
                .append(relationShipType)
                .append("`]->(end) return distinct labels(start),labels(end)");
        StatementResult result = execute(session, sb.toString());
        while (result.hasNext()) {
            Record record = result.next();
            try{
                String start = (String)record.get("labels(start)").asList().get(0);
                String end = (String)record.get("labels(end)").asList().get(0);
               /* if (start.contains("-")){
                    start = "`" + start + "`";
                }
                if (end.contains("-")){
                    end = "`" + end + "`";
                }*/
                RelationShip relationShip = new RelationShip();
                relationShip.setStart(start);
                relationShip.setEnd(end);
                relationShip.setRelationShip(relationShipType);
                relationShips.add(relationShip);
            }catch (IndexOutOfBoundsException e) {
                /**
                 * 为了应对这种数据
                 * labels(start)	labels(end)
                 * ["person"]	["standard"]
                 * ["person"]	[]
                 */
                log.error("-----------"+relationShipType);
            }
        }
        return relationShips;
    }

    /**
     * 查询该relation数据条数
     * @param session
     * @param relationShip
     * @return
     */
    public static int getCountByRelation(Session session, RelationShip relationShip){
        StringBuilder sb = new StringBuilder();
        int count = -1;
        List<Map<String, Object>> retResult = new ArrayList<>();
        sb.append("match ")
                .append("(start:`")
                .append(relationShip.getStart())
                .append("`)-[r:`")
                .append(relationShip.getRelationShip())
                .append("`]-(end:`")
                .append(relationShip.getEnd())
                .append("`) return count(r)");
        StatementResult result = execute(session, sb.toString());
        while(result.hasNext()){
            count = result.next().get("count(r)").asInt();
        }
        return count;
    }

    /**
     * 获取relation数据
     * @param session
     * @param cqlQueryBody
     * @return
     */
    public static List<Map<String, Object>> getRelationData(Session session, CqlQueryBody cqlQueryBody){
        StringBuilder sb = new StringBuilder();
        List<Map<String, Object>> retResult = new ArrayList<>();
        sb.append("match ")
                .append("(start:`")
                .append(cqlQueryBody.getRelationShip().getStart())
                .append("`)-[r:`")
                .append(cqlQueryBody.getRelationShip().getRelationShip())
                .append("`]-(end:`")
                .append(cqlQueryBody.getRelationShip().getEnd())
                .append("`) return id(start),id(end),r,id(r) skip ")
                .append(cqlQueryBody.getSkip())
                .append(" limit ")
                .append(cqlQueryBody.getLimit());
        StatementResult result = execute(session, sb.toString());
        while(result.hasNext()){
            Record record = result.next();
            Map<String, Object> temp = new HashMap<>(record.get("r").asMap());
            temp.put("neo4j_start_id", record.get("id(start)").asInt());
            temp.put("neo4j_end_id", record.get("id(end)").asInt());
            temp.put("neo4j_id", record.get("id(r)").asInt());
            retResult.add(temp);
        }
        return retResult;
    }

    /**
     * 获取relation数据
     * @param session
     * @param relationShip
     * @return
     */
    public static List<String> getRelationFields(Session session, RelationShip relationShip){
        StringBuilder sb = new StringBuilder();
        List<String> relationFields = new ArrayList<>();
        sb.append("match ")
                .append("(start:`")
                .append(relationShip.getStart())
                .append("`)-[r:`")
                .append(relationShip.getRelationShip())
                .append("`]-(end:`")
                .append(relationShip.getEnd())
                .append("`) return id(start),id(end),r,id(r) limit 1 ");
        StatementResult result = execute(session, sb.toString());
        while(result.hasNext()){
            Record record = result.next();
            Map<String, Object> temp = new HashMap<>(record.get("r").asMap());
            temp.put("neo4j_start_id", record.get("id(start)").asInt());
            temp.put("neo4j_end_id", record.get("id(end)").asInt());
            temp.put("neo4j_id", record.get("id(r)").asInt());
            relationFields.addAll(temp.keySet());
        }
        return relationFields;
    }


    /**
     * 获取node数据
     * @param session
     * @param cqlQueryBody
     * @return
     */
    public static List<Map<String, Object>> getNodeData(Session session, CqlQueryBody cqlQueryBody) {
        StringBuilder sb = new StringBuilder();
        List<Map<String, Object>> retResult = new ArrayList<>();
        sb.append("match (n:`")
                .append(cqlQueryBody.getLabel())
                .append("`) RETURN id(n),n  skip ")
                .append(cqlQueryBody.getSkip())
                .append(" limit ")
                .append(cqlQueryBody.getLimit());
        StatementResult result = execute(session, sb.toString());
        while (result.hasNext()) {
            Record record = result.next();
            HashMap<String, Object> temp = new HashMap<>(record.get("n").asMap());
            temp.put("neo4j_id", record.get("id(n)").asInt());
            retResult.add(temp);
        }
        return retResult;
    }

    /**
     * 获取node数据fields 属性为null的时候, 容易少字段
     * @param session
     * @param label
     * @return
     */
    public static List<String> getNodeFieldsBak(Session session, String label) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("match (n:`")
                .append(label)
                .append("`) RETURN id(n),n  limit 1 ");
        StatementResult result = execute(session, sb.toString());
        while (result.hasNext()) {
            Record record = result.next();
            HashMap<String, Object> temp = new HashMap<>(record.get("n").asMap());
            temp.put("neo4j_id", record.get("id(n)").asInt());
            fields.addAll(temp.keySet());
        }
        return fields;
    }

    /**
     * 获取node数据fields
     * @param session
     * @param label
     * @return
     */
    public static Set<String> getNodeFields(Session session, String label) {
        Map<String, Set<Neo4jTypeProperty>> nodeTypeProperties = getNodeTypeProperties(session);
        Set<String> ret = nodeTypeProperties.getOrDefault(label, new HashSet<>())
                .stream()
                .map(col -> col.getPropertyName())
                .collect(Collectors.toSet());
        ret.add("neo4j_id"); //neo4j_id不是人为定义的节点的属性, neo4j自动生成的
        return ret;
    }

    public static Map<String, Set<Neo4jTypeProperty>> getNodeTypeProperties(Session session) {

        Map<String, Set<Neo4jTypeProperty>> retMap = new HashMap<>();
        List<Neo4jTypeProperty> node4jTypePropertyList = new ArrayList<>();
        String cql = "call db.schema.nodeTypeProperties";
        StatementResult result = execute(session, cql);
        while (result.hasNext()) {
            Record record = result.next();
            List<Object> nodeLabels = record.get("nodeLabels").asList();
            for (Object nodeLabel : nodeLabels) {
                Neo4jTypeProperty node4jTypeProperty = new Neo4jTypeProperty();
                node4jTypeProperty.setNodeLabel((String) nodeLabel);
                node4jTypeProperty.setPropertyName(record.get("propertyName").asString());
                node4jTypePropertyList.add(node4jTypeProperty);
            }
        }
        Map<String, List<Neo4jTypeProperty>> var1 = node4jTypePropertyList.stream().collect(Collectors.groupingBy(Neo4jTypeProperty::getNodeLabel));
        var1.entrySet().stream().forEach(e -> retMap.put(e.getKey(), new HashSet<>(e.getValue())));

        return retMap;
    }

    /**
     * 　* @MethodName:     excute
     * 　* @Description:    执行Cypher查询
     * 　* @Param           [cql]
     * 　* @Return          org.neo4j.driver.v1.StatementResult
     * 　* @Author          王海波
     * 　* @Date            2019/4/24 14:12
     *
     * @Version v2.0
     */
    public static StatementResult execute(Session session, String cql) {
        StatementResult result = session.run(cql);
        return result;
    }

    public static StatementResult execute(Driver driver, String cql) {
        Session session = null;
        try {
            session = driver.session();
            return session.run(cql);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * close session
     *
     * @param session
     */
    public static void closeSession(Session session) {
        if (session != null) {
            session.close();
        }
    }

    public static void closeDriver(Driver driver) {
        if (driver != null) {
            driver.close();
        }
    }

}
