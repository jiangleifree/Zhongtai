package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.dao.SqlDao;
import cn.cnic.zhongtai.system.mapper.TopicInterfaceMapper;
import cn.cnic.zhongtai.system.mapper.TopicMapper;
import cn.cnic.zhongtai.system.model.InterfaceParam;
import cn.cnic.zhongtai.system.model.TopicInterface;
import cn.cnic.zhongtai.system.service.InterfaceParamService;
import cn.cnic.zhongtai.system.service.TopicInterfaceService;
import cn.cnic.zhongtai.utils.DBUtil;
import cn.cnic.zhongtai.utils.DataBaseType;
import cn.cnic.zhongtai.utils.ParamConvert;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Service
public class TopicInterfaceServiceImpl implements TopicInterfaceService {

    @Resource
    private TopicMapper topicMapper;
    @Resource
    private SqlDao sqlDao;
    @Resource
    private TopicInterfaceMapper topicInterfaceMapper;
    @Resource
    private InterfaceParamService interfaceParamService;

    @Override
    public List<Map<String, Object>> callInterface(String topicName, String tableName, String where) {
        return callInterface(topicName, tableName, where, "1", "1000");
    }

    @Override
    public List<Map<String, Object>> callInterface(String topicName, String tableName, String where, String page, String limit) {
        String dbName = topicMapper.getDBNameByTopicName(topicName);
        if (StringUtils.isEmpty(dbName)) {
            throw new RuntimeException("dbName is not exist");
        }
        return sqlDao.getData(dbName, tableName, where, page, limit);
    }

    @Override
    public List<TopicInterface> getAllByTopicName(String topicName) {
        return topicInterfaceMapper.getAllByTopicName(topicName);
    }

    @Override
    public String getInterfaceIdByUrl(String url) {
        return topicInterfaceMapper.getInterfaceIdByUrl(url);
    }

    @Override
    public int getTotalCount() {
        return topicInterfaceMapper.getTotalCount();
    }

    @Override
    public List<Map<String, Object>> getData(JSONObject jsonObject) {

        String topic = jsonObject.getString("topic");
        String table = jsonObject.getString("table");
        String from = jsonObject.getString("from");
        String size = jsonObject.getString("size");

        String dbName = topicMapper.getDBNameByTopicName(topic);
        if (StringUtils.isEmpty(dbName)) {
            throw new RuntimeException("dbName is not exist");
        }

        //api接口调用记录 添加
        String api = " /api/topicService/getDataByJson";

        StringBuilder sql = new StringBuilder();
        sql.append("select * from ");
        sql.append(dbName + "." + table);
        sql.append(" where ");
        parseSql(sql, jsonObject.getJSONObject("query"));
        sql.append(" limit ");
        sql.append(from + "," + size);

        //List<Map<String, Object>> ret = sqlDao.getJdbcTemplate().queryForList(sql.toString());
        System.out.println(sql.toString());
        return null;
    }

    @Override
    public List<Map<String, Object>> getList(String topicName, JSONObject jsonObject) {
        String table = jsonObject.getString("table");
        String from = jsonObject.getString("from");
        String size = jsonObject.getString("size");
        JSONArray fields = jsonObject.getJSONArray("fields");
        JSONArray groupBy = jsonObject.getJSONArray("group");
        JSONArray order = jsonObject.getJSONArray("order");


        String dbName = topicMapper.getDBNameByTopicName(topicName);
        if (StringUtils.isEmpty(dbName)) {
            throw new RuntimeException("dbName is not exist");
        }

        //拼接sql查询语句
        StringBuilder sql = new StringBuilder();

        sql.append("select ");
        sql.append(StringUtils.join(fields, ","));
        sql.append(" from ");
        sql.append(dbName + "." + table);
        sql.append(" where ");
        parseSql(sql, jsonObject.getJSONObject("query"));

        if (groupBy != null && groupBy.size() != 0) {
            sql.append(" group by ")
                    .append(StringUtils.join(groupBy, ","));
        }

        if (order != null && order.size() != 0) {
            String[] orderArr = new String[order.size()];
            for (int i = 0; i < order.size(); i++) {
                JSONObject ord = order.getJSONObject(i);
                for (String key : ord.keySet()) {
                    orderArr[i] = "" + key + " " + ord.getString(key);
                }
            }
            sql.append(" order by ")
                    .append(StringUtils.join(orderArr, ","));
        }

        if (StringUtils.isBlank(from)) {
            from = "0";
        }
        if (StringUtils.isBlank(size) || Integer.valueOf(size) > 1000) {
            size = "1000";
        }
        sql.append(" limit ");
        sql.append(from + "," + size);
        log.error(sql.toString());

        List<Map<String, Object>> ret = sqlDao.getJdbcTemplate().queryForList(sql.toString());

        return ret;
    }

    @Override
    public Integer getCount(String topicName, String tableName) {
        Integer count = null;
        String dbName = topicMapper.getDBNameByTopicName(topicName);
        if (StringUtils.isEmpty(dbName)) {
            throw new RuntimeException("dbName is not exist");
        }
        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) from ")
                .append(dbName)
                .append(".")
                .append(tableName);
        List<Map<String, Object>> maps = sqlDao.getJdbcTemplate().queryForList(sql.toString());
        for (Map<String, Object> map : maps) {
            for (String key : map.keySet()) {
                if (map.get(key) instanceof Integer) {
                    count = (Integer) map.get(key);
                }
            }
        }
        return count;
    }

    @Override
    public Boolean checkStatus(String url) {
        TopicInterface byUrl = topicInterfaceMapper.getByUrl(url);
        return byUrl.getStatus() == 1;
    }

    @Override
    public TopicInterface getByUrl(String url) {
        return topicInterfaceMapper.getByUrl(url);
    }


    @Override
    public List<Map<String, Object>> service(String topicName, String tag, HttpServletRequest request) {
        String uri = request.getRequestURI();
        InterfaceParam interfaceParam = interfaceParamService.getByUrl(uri);
        List<Map<String, Object>> retData = new ArrayList<>();

        //先把所有的{}替换换成? 并记录参数名,类型和位置
        parse(interfaceParam);

        String dbName = topicMapper.getDBNameByTopicName(topicName);
        if (StringUtils.isEmpty(dbName)) {
            throw new RuntimeException("param topic error");
        }

        String jdbcUrl = "jdbc:mysql://c:3307/" + dbName +
                "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowMultiQueries=true";
        Connection connection = DBUtil.getConnection(DataBaseType.MySql, jdbcUrl, "root", "xxx", 1);

        try {

            PreparedStatement pstmt = connection.prepareStatement(interfaceParam.getSql());

            List<String> sqlParams = interfaceParam.getSqlParams();
            List<InterfaceParam.Parameter> parameterList = interfaceParam.getParameterList();
            Map<String, String> paramMap = new HashMap<>(parameterList.size());
            for (InterfaceParam.Parameter parameter : parameterList) {
                paramMap.put(parameter.getName(), parameter.getType());
            }
            for (int i = 0; i < sqlParams.size(); i++) {
                //从request获取对应的参数值
                String val = request.getParameter(sqlParams.get(i));
                if (StringUtils.isEmpty(val)) {
                    throw new RuntimeException(sqlParams.get(i) + " must not be null");
                }
                //string int date
                String type = paramMap.get(sqlParams.get(i));
                if (StringUtils.isEmpty(type)) {
                    type = "string";
                }
                switch (type) {
                    case "string":
                        pstmt.setString(i + 1, val);
                        break;
                    case "int":
                        pstmt.setInt(i + 1, Integer.valueOf(val));
                        break;
                    case "double":
                        pstmt.setDouble(i + 1, Double.valueOf(val));
                        break;
                    case "date":
                        Date javaDate = (Date)ParamConvert.convert(val, Date.class);
                        java.sql.Date sqlDate = new java.sql.Date(javaDate.getTime());
                        pstmt.setDate(i + 1, sqlDate);
                        break;
                }

            }

            List<InterfaceParam.Parameter> responseList = interfaceParam.getResponseList();
            Map<String, String> respMap = new HashMap<>();
            for (InterfaceParam.Parameter parameter : responseList) {
                if (parameter.getName().equals("data")) {
                    for (InterfaceParam.Parameter pam : parameter.getItem()) {
                        respMap.put(pam.getName(), pam.getType());
                    }
                    break;
                }
            }

            ResultSet resultSet = pstmt.executeQuery();
            while (resultSet.next()) {
                Map<String, Object> data = new HashMap<>();
                for(String colName : respMap.keySet()) {
                    String type = respMap.get(colName);
                    if (StringUtils.isEmpty(type)) {
                        type = "string";
                    }
                    Object colValue;
                    switch (type) {
                        case "string":
                            colValue = resultSet.getString(colName);
                            break;
                        case "int":
                            colValue = resultSet.getInt(colName);
                            break;
                        case "date":
                            java.sql.Date sqlDate = resultSet.getDate(colName);
                            colValue = new Date(sqlDate.getTime());
                            break;
                        default:
                            colValue = resultSet.getString(colName);
                    }
                    data.put(colName, colValue);
                }
                retData.add(data);
            }

            return retData;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("sql error");
        } finally {
            if (connection != null) {
                DBUtil.closeDBResources(null, connection);
            }
        }

    }

    //替换{}为?, 并统计sql语句中的参数
    private void parse(InterfaceParam interfaceParam) {
        String sql = interfaceParam.getSql();
        if (StringUtils.isBlank(sql)) {
            throw new RuntimeException("sql param must not be null");
        }
        //替换{}
        List<String> params = new ArrayList<>();
        //替换{}
        int start = 0;
        int end = 0;
        while (true) {
            start = sql.indexOf("{", 0);
            if (start == -1) {
                break;
            }
            end = sql.indexOf("}", start);
            if (end == -1) {
                throw new RuntimeException("sql param error, no mapping }");
            }
            params.add(sql.substring(start + 1, end));
            sql = sql.replace(sql.substring(start, end + 1), "?");
        }

        List<InterfaceParam.Parameter> parameters = JSONArray.parseArray(interfaceParam.getParametersJsonStr(), InterfaceParam.Parameter.class);
        List<InterfaceParam.Parameter> response = JSONArray.parseArray(interfaceParam.getResponsesJsonStr(), InterfaceParam.Parameter.class);
        interfaceParam.setResponseList(response);
        interfaceParam.setParameterList(parameters);
        interfaceParam.setSqlParams(params);
        interfaceParam.setSql(sql);
    }


    private String parseParameters(HttpServletRequest request, InterfaceParam interfaceParam) {
        List<InterfaceParam.Parameter> parameters = JSONArray.parseArray(interfaceParam.getParametersJsonStr(), InterfaceParam.Parameter.class);
        Map<String, Object> requestParam = new HashMap<>();

        for (int i = 0; i < parameters.size(); i++) {
            Object val = request.getSession().getAttribute(parameters.get(i).getName());
            if (val == null) {
                throw new RuntimeException(parameters.get(i).getName() + " param is must not be null");
            }
            requestParam.put(parameters.get(i).getName(), val);
        }

        String sql = interfaceParam.getSql();
        for (String key : requestParam.keySet()) {
            sql = sql.replace("{" + key + "}", (String) requestParam.get(key));
        }
        return sql;
    }

    private void parseSql(StringBuilder sql, JSONObject jsonObject) {
        Tuple tuple = parseJsonObject(jsonObject);
        if (tuple != null) {
            sql.append(" ( ");
            for (int i = 0; i < tuple.jsonArray.size(); i++) {
                parseSql(sql, tuple.jsonArray.getJSONObject(i));
                if (i != tuple.jsonArray.size() - 1) {
                    sql.append(" " + tuple.oper + " ");
                }
            }
            sql.append(" ) ");
        }

        if (jsonObject.getJSONObject("EQ") != null) {
            Set<String> eq = jsonObject.getJSONObject("EQ").keySet();
            for (String key : eq) {
                sql.append(" " + key + " = \"" + jsonObject.getJSONObject("EQ").get(key) + "\"");
            }
        }
        if (jsonObject.getJSONObject("GT") != null) {
            Set<String> eq = jsonObject.getJSONObject("GT").keySet();
            for (String key : eq) {
                sql.append(" " + key + " > " + jsonObject.getJSONObject("GT").get(key));
            }
        }
        if (jsonObject.getJSONObject("LT") != null) {
            Set<String> eq = jsonObject.getJSONObject("LT").keySet();
            for (String key : eq) {
                sql.append(" " + key + " < " + jsonObject.getJSONObject("LT").get(key));
            }
        }
    }

    private Tuple parseJsonObject(JSONObject jsonObject) {
        boolean isExist = jsonObject.getJSONObject("bool") != null;
        Tuple result = null;
        if (isExist) {
            JSONArray and = jsonObject.getJSONObject("bool").getJSONArray("and");
            JSONArray or = jsonObject.getJSONObject("bool").getJSONArray("or");
            if (or != null) {
                result = new Tuple("or", or);
            } else if (and != null) {
                result = new Tuple("and", and);
            }
        }
        return result;
    }

    public static class Tuple {
        public final String oper;
        public final JSONArray jsonArray;

        Tuple(String oper, JSONArray jsonArray) {
            this.oper = oper;
            this.jsonArray = jsonArray;
        }
    }
}
