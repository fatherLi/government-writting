package com.myapp.governmentwritting.service;

import com.alibaba.fastjson2.JSONObject;
import com.myapp.governmentwritting.common.Result;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @description: AI 业务能力接口，封装大模型对话与文本处理核心能力
 * @author: Leung Chiu Wai
 * @date: 2025-06-23 02:03:31
 * @version: 1.0
 */
public interface AiService {
    
    /**
     * @description: 调用 FastGPT 进行流式对话，支持 SSE 或长连接实时输出
     * @author: Leung Chiu Wai
     * @date: 2025-06-23 02:03:31
     * @param: query 用户输入的提示词
     * @param: userId 用户唯一标识，用于鉴权与限流
     * @return: Flux<JSONObject> 包含增量内容的响应流
     */
    Flux<JSONObject> chatStreamWithFastGPT(String query, Long userId);

    /**
     * @description: 基于火山引擎大模型的文本校对能力，识别拼写、语法及语义错误
     * @author: Leung Chiu Wai
     * @date: 2025-08-01 14:15:22
     * @param: content 待处理的原始公文文本
     * @return: Mono<Result<String>> 校对后的建议文本及修改建议
     */
    Mono<Result<String>> proofread(String content);
}
