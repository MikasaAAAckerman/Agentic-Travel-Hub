//package com.travel.starter.config;
//
//import io.swagger.v3.oas.models.Components;
//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.info.Contact;
//import io.swagger.v3.oas.models.info.Info;
//import io.swagger.v3.oas.models.info.License;
//import io.swagger.v3.oas.models.security.SecurityRequirement;
//import io.swagger.v3.oas.models.security.SecurityScheme;
//import org.springdoc.core.models.GroupedOpenApi;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * Knife4j 接口文档配置类
// */
//@Configuration
//public class SwaggerConfig {
//
//
//    /**
//     * knife4j首页展示，默认地址：<a href="http://localhost:8080/doc.html#/home">...</a>
//     */
//    @Bean
//    public OpenAPI customOpenAPI() {
//        return new OpenAPI()
//                .info(new Info()
//                        .title("Agentic Travel Hub 接口文档")
//                        .description("暂时也没什么好介绍的，给家里人旅游用的一个策略小程序后端")
//                        .version("v1.0.0")
//                        .contact(new Contact()
//                                .name("老子自己")
//                                .url("https://github.com/Agentic-Travel-Hub"))
//                        .license(new License()
//                                .name("Apache 2.0")
//                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
//                // 添加全局安全要求（JWT Token）
//                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
//                // 配置安全方案
//                .components(new Components()
//                        .addSecuritySchemes("Authorization", createApiKeyScheme()));
//    }
//
//    /**
//     * 创建统一的token写入，懒得一个接口一个接口的复制了
//     */
//    private SecurityScheme createApiKeyScheme() {
//        return new SecurityScheme()
//                .type(SecurityScheme.Type.HTTP)
//                .scheme("bearer")
//                .bearerFormat("JWT")
//                .name("Authorization")
//                .in(SecurityScheme.In.HEADER)
//                .description("请输入 JWT Token，格式：Bearer {token}");
//    }
//
//
//    /**
//     * 定义“/api/v1/travel/**”为和智能体交互用的接口
//     */
//    @Bean
//    public GroupedOpenApi travelAgentApi() {
//        return GroupedOpenApi.builder()
//                .group("1. 智能体聊天类接口")
//                .pathsToMatch("/api/v1/travel/**")
//                .build();
//    }
//
//
//    /**
//     * 定义“/api/v1/system/**”为系统参数类的配置，懒得打开 Mysql改表了，直接swagger post完事
//     *
//     * @return
//     */
//    @Bean
//    public GroupedOpenApi systemApi() {
//        return GroupedOpenApi.builder()
//                .group("2. 系统基础接口")
//                .pathsToMatch("/api/v1/system/**")
//                .build();
//    }
//
//
//    /**
//     * 心跳检测用的接口，做最简单的通路测试，什么模型简易对话啊，服务心跳检测啊都往里塞
//     *
//     * @return
//     */
//    @Bean
//    public GroupedOpenApi publicApi() {
//        return GroupedOpenApi.builder()
//                .group("3. 心跳测试接口")
//                .pathsToMatch("/api/v1/health-test/**")
//                .build();
//    }
//}
