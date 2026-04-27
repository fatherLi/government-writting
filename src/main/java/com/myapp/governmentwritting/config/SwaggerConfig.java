package com.myapp.governmentwritting.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * @description: Swagger配置类
 * @author: Leung Chiu Wai
 * @date: 2025-09-12 19:05:39
 * @version: 1.0
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30) // 使用 Swagger 3.0 标准
                .apiInfo(apiInfo())
                .select()
                // 扫描控制器所在的包，替换成你实际的 controller 包名
                .apis(RequestHandlerSelectors.basePackage("com.ubdi.governmentwritting.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("石景山政务公文系统 - 接口文档")
                .description("后端接口定义与调试说明")
                .contact(new Contact("Li Hao", "", ""))
                .version("1.0")
                .build();
    }
}
