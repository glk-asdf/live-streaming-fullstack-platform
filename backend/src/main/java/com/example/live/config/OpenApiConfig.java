package com.example.live.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 文档配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI liveOpenApi() {
        return new OpenAPI().info(new Info()
                .title("直播全栈平台 API")
                .description("推流、播放、弹幕、礼物、回放与管理接口")
                .version("v0.1")
                .contact(new Contact().name("live-streaming-platform")));
    }
}
