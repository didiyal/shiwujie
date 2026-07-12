package com.swj.shiwujie.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc/Knife4j4 OpenAPI 配置（v3.0.0 阶段1.4：取代 v2.1.0 三模块各自的 springfox Docket SwaggerConfig）。
 * <p>全局一份置于 common-web，各业务服务经组件扫描（com.swj.shiwujie）生效，自动扫描 controller 包下的接口；
 * 全局声明 Bearer JWT 安全方案，对应登录鉴权的 Authorization 头。阶段 2 合并单体后仍是这一份。
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer JWT";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("视无界接口文档")
                        .description("无障碍服务平台对外接口（单体化后契约继承 v2.1.0）")
                        .version("3.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .schemaRequirement(SECURITY_SCHEME_NAME, new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER));
    }
}
