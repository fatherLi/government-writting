package com.myapp.governmentwritting.handler;

import com.myapp.governmentwritting.service.DataService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataSyncHandler extends TextWebSocketHandler {

    // 用于存储所有在线的 WebSocket 会话
    private static final ConcurrentHashMap<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    private final ApplicationContext applicationContext;
    private DataService dataService;

    // 注入 ApplicationContext (Spring 核心上下文，绝不会引起循环依赖)
    public DataSyncHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // 懒加载获取 DataService
    private DataService getDataService() {
        if (this.dataService == null) {
            this.dataService = applicationContext.getBean(DataService.class);
        }
        return this.dataService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // 1. 调用 Service 进行异步入库
        getDataService().asyncSave(payload);

        // 2. 回复客户端消息已收到 (可选)
        session.sendMessage(new TextMessage("服务器已收到坐标数据"));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SESSIONS.put(session.getId(), session);
        System.out.println("新连接已建立 ID: " + session.getId() + ", 当前连接数: " + SESSIONS.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SESSIONS.remove(session.getId());
        System.out.println("连接已关闭 ID: " + session.getId() + ", 原因: " + status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        SESSIONS.remove(session.getId());
        System.err.println("传输错误 ID: " + session.getId() + ", 错误: " + exception.getMessage());
    }

    // === 下面是你自己写的实用工具方法 ===

    public void broadcast(String message) throws IOException {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : SESSIONS.values()) {
            if (session.isOpen()) {
                synchronized (session) { // 防止并发发送报错
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    public void sendToSession(String sessionId, String message) throws IOException {
        WebSocketSession session = SESSIONS.get(sessionId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    public int getConnectedCount() {
        return SESSIONS.size();
    }
}