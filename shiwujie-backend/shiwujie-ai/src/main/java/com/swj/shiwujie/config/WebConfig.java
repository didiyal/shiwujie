package com.swj.shiwujie.config;

import com.swj.shiwujie.interceptor.AiLoginCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * v3.0.0 单体化阶段2.5：ai 拦截器仅作用于 /api/ai/**（ai 行为独立，见 {@link AiLoginCheckInterceptor}）。
 * CORS 由 common-web 的 WebConfig 统一配置，此处不再重复。
 */
@Configuration //配置类
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AiLoginCheckInterceptor aiLoginCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(aiLoginCheckInterceptor)
                .addPathPatterns("/api/ai/**");
    }

}
