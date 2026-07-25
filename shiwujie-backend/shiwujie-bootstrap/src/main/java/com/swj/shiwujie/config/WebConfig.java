package com.swj.shiwujie.config;

import com.swj.shiwujie.interceptor.LoginCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * v3.0.0 单体化阶段2.5：收敛 user/call/community 三份 WebConfig 为 common-web 一份。
 * 业务拦截器仅作用于 /api/{user,call,community}/**；ai 路径（/api/ai/**）由 ai 模块自带
 * 的 WebConfig 注册其独立的 AiLoginCheckInterceptor（ai 行为不同：dev 默认用户兜底，属安全加固出范围，保留）。
 * 全局 CORS 收归此处统一配置。
 */
@Configuration //配置类
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginCheckInterceptor loginCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginCheckInterceptor)
                .addPathPatterns("/api/user/**", "/api/call/**", "/api/community/**")
                .excludePathPatterns(
                        "/doc.html", "/swagger-ui.html", "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs", "/v3/api-docs/**",
                        "/favicon.ico",
                        "/error",
                        "/webjars/**");
    }


    /**
     * 跨域配置
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
//                .allowedOriginPatterns("http://*:5173") // 允许 localhost 不同端口
                .allowedOriginPatterns(
                        "http://*:*",     // 允许所有 http 端口
                        "https://*:*",    // 允许所有 https 端口
                        "https://www.shi-wu-jie.xyz", // 特定域名
                        "file://*"        // 允许本地文件
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true);
    }


}
