package com.myapp.governmentwritting.config;

import com.myapp.governmentwritting.handler.DataSyncHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DataSyncHandler dataSyncHandler;

    // 使用 @Lazy 解决循环依赖：启动时先不实例化 Handler，等有人连接时才真正创建
    public WebSocketConfig(@Lazy DataSyncHandler dataSyncHandler) {
        this.dataSyncHandler = dataSyncHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(dataSyncHandler, "/ws/realtime")
                .setAllowedOriginPatterns("*"); // 允许所有跨域请求
    }
}