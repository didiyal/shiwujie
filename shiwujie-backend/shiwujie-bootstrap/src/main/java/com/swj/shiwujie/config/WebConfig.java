package com.swj.shiwujie.config;

import com.swj.shiwujie.interceptor.LoginCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * v3.0.0 单体化阶段2.5：收敛 user/call/community 三份 WebConfig 为 common-web 一份。
 * 业务拦截器仅作用于 /api/{user,call,community}/**。AI 通道（原 /api/ai/** SSE 端点）已随
 * Java AI 模块重写删除（2b-5 / 2b-6a），原 AiLoginCheckInterceptor + AiWebConfig 已删，鉴权统一回归
 * Java WS 网关 /api/ws/call（ticket 机制见 [auth.md](../../../../docs/architecture/auth.md)）。
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
