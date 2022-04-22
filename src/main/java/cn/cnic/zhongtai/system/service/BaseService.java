/*
package cn.cnic.zhongtai.system.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.*;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.util.WebUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


*/
/**
 * @Title: BaseService.java
 * @Project: csai-api
 * @Description: ES基础查询基Service
 * @Author 路长发
 * @Date 2019/3/27 15:18
 * @Version V1.0
 *//*

@Transactional(readOnly = true)
@Service
@Slf4j
public class  BaseService {

    private final String PREFIX = "query_";

    @Resource
    private ElasticsearchTemplate elasticsearchTemplate;

    */
/**
     * 通过表达式和属性值创建查询过滤器
     *
     * <code>
     * QueryFilterUtils.create("EQ_propertyName","value")
     * </code>
     *
     * @param expression    表达式
     * @param propertyValue 属性值
     * @return {@link QueryFilter}
     *//*

    public static QueryFilter create(String expression, Object propertyValue) {
        Assert.notNull(expression, "expression is null");
        Assert.notNull(propertyValue, "propertyValue is null");

        // 拆分 operator 与 propertyName
        String[] expressions = StringUtils.split(expression, "_");

        Operator operator = Operator.valueOf(expressions[0]);

        String propertyName = "";
        if (expressions.length == 2) {
            propertyName = expressions[1];
        } else if (expressions.length == 3) {
            propertyName = expressions[1] + "_" + expressions[2];
        } else if (expressions.length == 4) {
            propertyName = expressions[1] + "_" + expressions[2] + "_" + expressions[3];
        } else if (expressions.length == 5) {
            propertyName = expressions[1] + "_" + expressions[2] + "_" + expressions[3] + "_" + expressions[4];
        } else {
            throw new RuntimeException("invalid expression " + expressions.toString());
        }
        String nestedPath = null;
        if (propertyName.contains(".")) {
            nestedPath = propertyName.split("\\.")[0];
        }
        QueryFilter filter = new QueryFilter(propertyName, propertyValue, operator, nestedPath);

        return filter;

    }

    */
/**
     * 通过 Map 创建 QueryFilter 列表
     * map.put("EQ_propertyName","value")
     *
     * @param queryParams 查询参数
     * @return List<QueryFilter>
     *//*

    public static List<QueryFilter> create(Map<String, Object> queryParams) {
        List<QueryFilter> filters = Lists.newArrayList();
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            String expression = entry.getKey();
            Object value = entry.getValue();
            if (value == null || StringUtils.isBlank(value.toString())) {
                continue;
            }
            QueryFilter filter = create(expression, value);
            filters.add(filter);
        }
        return filters;
    }

    */
/**
     * 获取请求全部参数
     * 请求参数示例1 query_EQ_name  精确查询name
     * 请求参数示例2 query_EQ_org_name 精确查询org_name
     * 请求参数示例3 offset 起始行
     * 请求参数示例4 limit 每页显示数据量
     *
     * @param request
     * @return
     *//*

    public List<QueryFilter> createQueryFilter(HttpServletRequest request, String queryPrefix) {
        Map<String, Object> filterParamMap = WebUtils.getParametersStartingWith(request, queryPrefix);
        return create(filterParamMap);
    }

    */
/**
     * 　* @MethodName: createQueryFilter
     * 　* @Description: 默认查询处理
     * 　* @Param
     * 　* @Return
     * 　* @Author 路长发
     * 　* @Date 2019/3/27 15:54
     *//*

    public List<QueryFilter> createQueryFilter(HttpServletRequest request) {
        return this.createQueryFilter(request, PREFIX);
    }

    */
/**
     * 　* @MethodName:     createQueryFilter
     * 　* @Description:    生成查询过滤器
     * 　* @Param           [parameters]
     * 　* @Return          java.util.List<cn.cnic.csai.util.QueryFilter>
     * 　* @Author          王海波
     * 　* @Date            2019/8/19 21:55
     *
     * @Version v2.0
     *//*

    public List<QueryFilter> createQueryFilter(Map<String, Object> parameters) {
        List<QueryFilter> filters = Lists.newArrayList();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String expression = entry.getKey();
            Object value = entry.getValue();
            if (value == null || StringUtils.isBlank(value.toString())) {
                continue;
            }
            QueryFilter filter = create(expression, value);
            filters.add(filter);
        }
        return filters;
    }

    */
/**
     * 根据查询参数构建 组合查询
     * 2019-06-23 路长发 支持EQ对一个参数多值查询
     * a^b   a|b
     *
     * @param filters
     * @return
     *//*

    public QueryBuilder createQueryBuilder(List<QueryFilter> filters) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        List<QueryBuilder> parentBuilders = new ArrayList<>();
        Map<String, List<QueryBuilder>> map = new HashMap<>();

        for (QueryFilter filter : filters) {
            QueryBuilder queryBuilder = null;
            switch (filter.getOperator()) {
                case EQ:
                    if (filter.getPropertyValue().toString().contains("^")) {
                        BoolQueryBuilder bqMust = new BoolQueryBuilder();
                        Object[] values = filter.getPropertyValue().toString().split("\\^");
                        for (Object value : values) {
                            bqMust.must(QueryBuilders.matchPhraseQuery(filter.getPropertyName(), value));
                        }
                        queryBuilder = bqMust;
                    } else if (filter.getPropertyValue().toString().contains("|")) {
                        BoolQueryBuilder bqShould = new BoolQueryBuilder();
                        Object[] values = filter.getPropertyValue().toString().split("\\|");
                        for (Object value : values) {
                            bqShould.should(QueryBuilders.matchPhraseQuery(filter.getPropertyName(), value));
                        }
                        queryBuilder = bqShould;
                    } else {
                        queryBuilder = QueryBuilders.matchPhraseQuery(filter.getPropertyName(), filter.getPropertyValue());
                    }
                    break;
                case TERM:
                    queryBuilder = QueryBuilders.termsQuery(filter.getPropertyName(), filter.getPropertyValue());
                    break;
                case LK:
                    String q = filter.getPropertyValue().toString();
                    queryBuilder = QueryBuilders.matchQuery(filter.getPropertyName(), q);
                    break;
                case ALL:
                    queryBuilder = QueryBuilders.multiMatchQuery(filter.getPropertyValue(), filter.getPropertyName());
                    break;
                case GT:
                    queryBuilder = QueryBuilders.rangeQuery(filter.getPropertyName()).gt(filter.getPropertyValue());
                    break;
                case GTE:
                    queryBuilder = QueryBuilders.rangeQuery(filter.getPropertyName()).gte(filter.getPropertyValue());
                    break;
                case LT:
                    queryBuilder = QueryBuilders.rangeQuery(filter.getPropertyName()).lt(filter.getPropertyValue());
                    break;
                case LTE:
                    queryBuilder = QueryBuilders.rangeQuery(filter.getPropertyName()).lte(filter.getPropertyValue());
                    break;
                default:
                    break;
            }

            if (filter.getNestedPath() != null) {
                List<QueryBuilder> mapList = map.get(filter.getNestedPath()) == null ? new ArrayList<>() : map.get(filter.getNestedPath());
                mapList.add(queryBuilder);
                map.put(filter.getNestedPath(), mapList);
            } else {
                if (queryBuilder != null) {
                    parentBuilders.add(queryBuilder);
                }
            }
        }

        //单独处理nest内部查询

        for (Map.Entry<String, List<QueryBuilder>> entry : map.entrySet()) {
            BoolQueryBuilder bqb = new BoolQueryBuilder();
            if (entry.getValue() != null) {
                for (QueryBuilder q : entry.getValue()) {
                    bqb.must(q);
                }
            }
            NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery(entry.getKey(), bqb, ScoreMode.Max);
            boolQueryBuilder.must(nestedQueryBuilder);
        }

        for (QueryBuilder b : parentBuilders) {
            boolQueryBuilder.must(b);
        }

        return boolQueryBuilder;
    }

    */
/**
     * 单一源的聚合查询
     *
     * @param queryBuilder
     * @param term
     * @param property
     * @param repository
     * @param size         为0表示没有限制 >0 则表示
     * @return
     *//*

    public Map<String, Object> aggeration(QueryBuilder queryBuilder, String term, String property, ElasticsearchRepository repository, int size) {
        SearchQuery searchQuery;
        if (size <= 0) {
            searchQuery = creatSearchQuery(queryBuilder, AggregationBuilders.terms(term).order(BucketOrder.count(false)).field(property + ".keyword"));
        } else {
            searchQuery = creatSearchQuery(queryBuilder, AggregationBuilders.terms(term).order(BucketOrder.count(false)).field(property + ".keyword").size(size));
        }

        AggregatedPage page = (AggregatedPage) repository.search(searchQuery);

        Terms patentType = (Terms) page.getAggregations().getAsMap().get(term);
        Map<String, Object> patentTypeMap = new HashMap<>();
        for (Terms.Bucket bucket : patentType.getBuckets()) {
            if (bucket.getKey() != null) {
                patentTypeMap.put(bucket.getKey().toString(), bucket.getDocCount());
            }

        }

        return patentTypeMap;
    }


    */
/**
     * nested 聚合结果处理公用方法
     *
     * @param index
     * @param type
     * @param nestedName
     * @param path
     * @param property
     * @param queryBuilder
     * @return
     *//*

    public Map<String, Object> aggerationNested(String index, String type, String nestedName, String path, String property, QueryBuilder queryBuilder) {

        SearchRequestBuilder search = createNestedSearchQuery(index, type, nestedName, path, queryBuilder,
                AggregationBuilders.terms("name").order(BucketOrder.count(false)).field(path + "." + property + ".keyword"));

        Nested ntd = search.get().getAggregations().get(nestedName);
        Terms term = ntd.getAggregations().get("name");

        Map<String, Object> map = new HashMap<>();
        for (Terms.Bucket bucket : term.getBuckets()) {
            map.put(bucket.getKey().toString(), bucket.getDocCount());
        }

        return map;
    }

    */
/**
     * 构建查询分页信息
     *
     * @param request   http请求
     * @param direction 排序 DECS  ASC
     * @param filedName 排序字段名
     * @return
     *//*

    public Pageable createPageInfo(HttpServletRequest request, Sort.Direction direction, String filedName) {
        String offset = request.getParameter(Constants.Query.REQUEST_PAGE_OFFSET);
        String pageSize = request.getParameter(Constants.Query.REQUEST_PAGE_LIMIT);
        int page = 0;
        if (offset != null) {
            page = Integer.valueOf(offset);
            if (page >= 1000) {
                page = 999;
            }
        } else {
            page = 0;
        }
        int size = 0;
        if (pageSize != null) {
            size = Integer.valueOf(pageSize);
        } else {
            size = 10;
        }
        return PageRequest.of(page, size, Sort.by(direction, filedName));
    }


    */
/**
     * 　* @MethodName:     createPageInfo
     * 　* @Description:    构建查询分页信息
     * 　* @Param           [request, direction, filedName]
     * 　* @Return          org.springframework.data.domain.Pageable
     * 　* @Author          王海波
     * 　* @Date            2019/8/19 22:23
     *
     * @Version v2.0
     *//*

    public Pageable createPageInfo(String offset, String pageSize, Sort.Direction direction, String filedName) {
        int page = 0;
        if (offset != null) {
            page = Integer.valueOf(offset);
            if (page >= 1000) {
                page = 999;
            }
        } else {
            page = 0;
        }
        int size = 0;
        if (pageSize != null) {
            size = Integer.valueOf(pageSize);
        } else {
            size = 10;
        }
        return PageRequest.of(page, size, Sort.by(direction, filedName));
    }

    */
/**
     * 统计页面 top 分页信息
     *
     * @param offset
     * @param pageSize
     * @param direction
     * @param filedName
     * @return
     *//*

    public Pageable createPageInfo(int offset, int pageSize, Sort.Direction direction, String filedName) {
        int page = Integer.valueOf(offset);
        int size = Integer.valueOf(pageSize);
        return PageRequest.of(page, size, Sort.by(direction, filedName));
    }

    */
/**
     * 创建SearchQuery对象 用于聚合（非nested）查询
     *
     * @param queryBuilder
     * @param aggregationBuilder
     * @return
     *//*

    public SearchQuery creatSearchQuery(QueryBuilder queryBuilder, AbstractAggregationBuilder... aggregationBuilder) {
        NativeSearchQueryBuilder nsqb = new NativeSearchQueryBuilder();
        NativeSearchQueryBuilder nativeSearchQueryBuilder = nsqb.withQuery(queryBuilder);
        for (AbstractAggregationBuilder abstractAggregationBuilder : aggregationBuilder) {
            nativeSearchQueryBuilder = nativeSearchQueryBuilder.addAggregation(abstractAggregationBuilder);
        }
        return nativeSearchQueryBuilder.build();
    }

    */
/**
     * 　* @MethodName: creatNestedSearchQuery
     * 　* @Description: 构造nested的内聚
     * 　* @Param
     * 　* @Return
     * 　* @Author 路长发
     * 　* @Date 2019/3/28 9:54
     *//*

    public SearchRequestBuilder createNestedSearchQuery(String index, String type, String nestedName, String path, QueryBuilder queryBuilder, AbstractAggregationBuilder... aggregationBuilder) {
        NestedAggregationBuilder nb = AggregationBuilders.nested(nestedName, path);
        for (AbstractAggregationBuilder abstractAggregationBuilder : aggregationBuilder) {
            nb.subAggregation(abstractAggregationBuilder);
        }
        SearchRequestBuilder search = elasticsearchTemplate.getClient().prepareSearch(index).setTypes(type);
        search.setQuery(queryBuilder);  // 按照条件做聚合
        search.addAggregation(nb);
        return search;
    }

    */
/**
     * 　* @MethodName: getListMap
     * 　* @Description: 将list对象转换为es支持的 List<Map<String, Object>> 方便更新操作
     * 　* @Param
     * 　* @Return
     * 　* @Author 路长发
     * 　* @Date 2019/4/10 16:48
     *//*

    public List<Map<String, Object>> getListMap(List setArry) throws IllegalAccessException {
        List<Map<String, Object>> set = Lists.newArrayList();
        Map<String, Object> map = Maps.newHashMap();
        Class<?> clazz;
        for (Object obj : setArry) {
            clazz = obj.getClass();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                String name = field.getName();
                Object value = field.get(obj);
                map.put(name, value);
            }
            set.add(map);
            map = Maps.newHashMap();
        }
        return set;
    }

    */
/**
     * 　* @MethodName: builAggregationVo
     * 　* @Description: 将聚合数据返回给前端需要的格式
     * 　* @Param
     * 　* @Return
     * 　* @Author 路长发
     * 　* @Date 2019/4/10 17:35
     *//*

    public HashMap<String, List<GroupInfo>> builAggregationVo(Map<String, Object> totalMap) {
        HashMap<String, List<GroupInfo>> groupFilterMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : totalMap.entrySet()) {
            List<GroupInfo> groupInfos = new ArrayList<>();
            String key = entry.getKey();
            Map<String, Object> map = (Map<String, Object>) entry.getValue();
            for (Map.Entry<String, Object> entry1 : map.entrySet()) {
                GroupInfo groupInfo = new GroupInfo();
                groupInfo.setName(entry1.getKey());
                groupInfo.setValue(Integer.parseInt(entry1.getValue().toString()));
                groupInfos.add(groupInfo);
            }
            groupFilterMap.put(key, groupInfos);
        }
        return groupFilterMap;
    }


    */
/**
     * 　* @MethodName: addChildrenQuery
     * 　* @Description: 构造子查询EQ,该版本中已弃用parent-children组织形式
     * 　* @Param
     * 　* @Return
     * 　* @Author 路长发
     * 　* @Date 2019/3/21 10:23
     *//*

    public QueryBuilder addChildrenQuery(String filedName, String value, String type) {
        if (StringUtils.isNotEmpty(value)) {
            MatchPhraseQueryBuilder builder = QueryBuilders.matchPhraseQuery(filedName, value);
            QueryBuilder queryBuilder = JoinQueryBuilders.hasChildQuery(type, builder, ScoreMode.Max);
            return queryBuilder;
        }
        return null;
    }

    public QueryBuilder addChildrenQueryGTE(String value, String filedName, String type) {
        if (StringUtils.isNotEmpty(value)) {
            RangeQueryBuilder rangeGTE = QueryBuilders.rangeQuery(filedName).gte(value);
            QueryBuilder queryBuilder = JoinQueryBuilders.hasChildQuery(type, rangeGTE, ScoreMode.Max);
            return queryBuilder;
        }
        return null;
    }

    public QueryBuilder addChildrenQueryLTE(String value, String filedName, String type) {
        if (StringUtils.isNotEmpty(value)) {
            RangeQueryBuilder rangeLTE = QueryBuilders.rangeQuery(filedName).lte(value);
            QueryBuilder queryBuilder = JoinQueryBuilders.hasChildQuery(type, rangeLTE, ScoreMode.Max);
            return queryBuilder;
        }
        return null;
    }


}
*/
