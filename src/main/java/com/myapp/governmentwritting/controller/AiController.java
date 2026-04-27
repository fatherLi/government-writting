package com.myapp.governmentwritting.controller;

import com.alibaba.fastjson2.JSONObject;
import com.myapp.governmentwritting.common.Result;
import com.myapp.governmentwritting.common.SecurityUtils;
import com.myapp.governmentwritting.service.AiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * @description: Ai控制器，处理相关HTTP请求
 * @author: Leung Chiu Wai
 * @date: 2025-08-01 14:13:26
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {


    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    // 大模型智能问话 (FastGPT) - 流式返回
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        /**
     * @description: 执行chatWithFastGPT相关业务逻辑
     * @author: Leung Chiu Wai
     * @date: 2025-06-29 03:25:48
     * @param: request
     * @return: Flux<JSONObject>
     */
    public Flux<JSONObject> chatWithFastGPT(@RequestBody JSONObject request) {
        String query = request.getString("query");
        if (!StringUtils.hasText(query)) {
            log.warn("FastGPT聊天请求失败：提问内容为空");
            JSONObject error = new JSONObject();
            error.put("error", "提问内容不能为空");
            return Flux.just(error);
        }
        
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("接收到FastGPT聊天请求，用户ID: {}, 提问内容长度: {}", userId, query.length());
        
        return aiService.chatStreamWithFastGPT(query, userId)
                .onErrorResume(e -> {
                    log.error("FastGPT聊天流处理异常", e);
                    JSONObject error = new JSONObject();
                    error.put("error", "AI服务内部异常，请稍后再试");
                    return Flux.just(error);
                });
    }

    // 火山引擎 智能校对 (一期WebFlux实现，用于内容、格式校对)
    /**
     * @description: 执行proofread相关业务逻辑
     * @author: Leung Chiu Wai
     * @date: 2025-08-06 23:38:31
     * @param: request
     * @return: Mono<Result<String>>
     */
    @PostMapping("/proofread")
    public Mono<Result<String>> proofread(@RequestBody JSONObject request) {
        String content = request.getString("content");
        if (!StringUtils.hasText(content)) {
            log.warn("火山引擎校对请求失败：校对内容为空");
            return Mono.just(Result.error(400, "校对内容不能为空"));
        }
        
        log.info("接收到火山引擎智能校对请求，文本长度: {}", content.length());
        
        return aiService.proofread(content)
                .onErrorResume(e -> {
                    log.error("火山引擎校对服务调用异常", e);
                    return Mono.just(Result.error(500, "智能校对服务暂不可用，请稍后再试"));
                });
    }
}
