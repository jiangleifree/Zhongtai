package cn.cnic.zhongtai.system.config;

import cn.cnic.zhongtai.system.interceptor.AccessStatisticsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebAppConfig implements WebMvcConfigurer {

    @Resource
    private AccessStatisticsInterceptor statisticsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(statisticsInterceptor)
                .addPathPatterns("/api/topicService/*/*")
                .addPathPatterns("/map/api/mapStorageService/*");

    }
}
