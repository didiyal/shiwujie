//package com.swj.shiwujie.config;
//
//import com.swj.shiwujie.interceptor.LoginCheckInterceptor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration //配置类
//public class WebConfig implements WebMvcConfigurer {
//
//    @Autowired
//    private LoginCheckInterceptor loginCheckInterceptor;
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(loginCheckInterceptor).addPathPatterns("/**").excludePathPatterns(
//                "/doc.html","/swagger-ui.html",
//                "/swagger-resources/**",
//                "/v2/api-docs",
//                "/favicon.ico",
//                "/error",
//                "/webjars/**");
//    }
//
//
//    /**
//     * 跨域配置
//     * @param registry
//     */
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
////                .allowedOriginPatterns("http://*:5173") // 允许 localhost 不同端口
//                .allowedOriginPatterns(
//                        "http://*:*",     // 允许所有 http 端口
//                        "https://*:*",    // 允许所有 https 端口
//                        "file://*"        // 允许本地文件
//                )
//                .allowedMethods("GET", "POST", "PUT", "DELETE")
//                .allowedHeaders("Authorization", "Content-Type")
//                .allowCredentials(true);
//    }
//
//
//}
//
