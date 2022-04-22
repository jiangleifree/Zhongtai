package cn.cnic.zhongtai.system.interceptor;

import cn.cnic.zhongtai.system.model.InterfaceParam;
import cn.cnic.zhongtai.system.model.TopicInterfaceStatistics;
import cn.cnic.zhongtai.system.service.InterfaceParamService;
import cn.cnic.zhongtai.system.service.TopicInterfaceStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
@Slf4j
public class AccessStatisticsInterceptor extends HandlerInterceptorAdapter {

    @Resource
    private TopicInterfaceStatisticsService statisticsService;
    @Resource
    private InterfaceParamService interfaceService;

    /**
     * 注意这里不能关闭流
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        InterfaceParam interfaceParam = interfaceService.getByUrl(requestURI);
        if (interfaceParam == null) {
            throw new RuntimeException("接口未开放, 请联系管理员进行激活");
        }

        //添加查询记录
        TopicInterfaceStatistics statistic = new TopicInterfaceStatistics();
        statistic.setCallTime(new Date());
        statistic.setUserIp(request.getRemoteAddr());
        statistic.setTopicInterfaceId(interfaceParam.getId());
        statisticsService.create(statistic);

        return true;
    }
}
