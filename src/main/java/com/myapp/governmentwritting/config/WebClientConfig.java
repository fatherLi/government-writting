package com.myapp.governmentwritting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * @description: WebClient配置类，用于项目配置
 * @author: Leung Chiu Wai
 * @date: 2025-08-13 21:43:41
 * @version: 1.0
 */
@Configuration
public class WebClientConfig {

    /**
     * @description: 初始化并装配全局非阻塞的响应式HTTP客户端 WebClient
     * @author: Leung Chiu Wai
     * @date: 2025-08-22 16:31:08
     * @return: WebClient 具备企业级连接池和超时控制的WebClient实例
     */
    @Bean
    public WebClient webClient() {
        // 配置企业级 HTTP Client，针对高并发大模型请求做超时等处理
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60));
                
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
