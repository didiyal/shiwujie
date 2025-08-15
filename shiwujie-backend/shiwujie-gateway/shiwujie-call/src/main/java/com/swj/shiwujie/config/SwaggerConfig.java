//package com.swj.shiwujie.config;
//
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import springfox.documentation.builders.ApiInfoBuilder;
//import springfox.documentation.builders.ParameterBuilder;
//import springfox.documentation.builders.PathSelectors;
//import springfox.documentation.builders.RequestHandlerSelectors;
//import springfox.documentation.schema.ModelRef;
//import springfox.documentation.service.ApiInfo;
//import springfox.documentation.spi.DocumentationType;
//import springfox.documentation.spring.web.plugins.Docket;
//import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;
//
//import java.util.Arrays;
//
//@Configuration
//@EnableSwagger2WebMvc
//public class SwaggerConfig {
//
//    @Bean(value = "defaultApi2")
//    public Docket defaultApi2() {
//        return new Docket(DocumentationType.SWAGGER_2)
//                .apiInfo(apiInfo())
//                .select()
//                // 这里一定要标注你控制器的位置
//                .apis(RequestHandlerSelectors.basePackage("com.swj.shiwujie.controller"))
//                .paths(PathSelectors.any())
//                .build().groupName("call")
//                .globalOperationParameters(
//                        Arrays.asList(new ParameterBuilder()
//                                .name("Authorization")
//                                .description("JWT令牌")
//                                .modelRef(new ModelRef("string"))
//                                .parameterType("header") // 设置请求头参数
//                                .required(false) // 是否为必填
//                                .build()
//                        ));
//    }
//
//    /**
//     * api 信息
//     * @return
//     */
//    private ApiInfo apiInfo() {
//        return new ApiInfoBuilder()
//                .title("视频模块接口文档")
//                .description("视频的相关操作与志愿者匹配")
//                .termsOfServiceUrl("https://gitee.com/didyal/projects")
//                .version("1.0")
//                .build();
//    }
//}
//
