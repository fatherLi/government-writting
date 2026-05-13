package com.myapp.governmentwritting.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.myapp.governmentwritting.common.Result;
import com.myapp.governmentwritting.entity.AiChatLog;
import com.myapp.governmentwritting.mapper.AiChatLogMapper;
import com.myapp.governmentwritting.service.AiService;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @description: AI服务核心实现类，封装与FastGPT及火山引擎的大模型交互逻辑
 * @author: Leung Chiu Wai
 * @date: 2025-07-15 10:30:00
 * @version: 1.0
 */
@Service
public class AiServiceImpl implements AiService {


    private final WebClient webClient;


    private final AiChatLogMapper aiChatLogMapper;


    private final RedisTemplate<String, Object> redisTemplate;

    private final ThreadPoolTaskExecutor asyncExecutor;


    private final RedissonClient redissonClient;

    @Value("${fastgpt.url}")
    private String fastgptUrl;

    @Value("${fastgpt.key}")
    private String fastgptKey;

    @Value("${volcengine.url}")
    private String volcengineUrl;

    @Value("${volcengine.key}")
    private String volcengineKey;

    public AiServiceImpl(WebClient webClient, AiChatLogMapper aiChatLogMapper, RedisTemplate<String, Object> redisTemplate, ThreadPoolTaskExecutor asyncExecutor, RedissonClient redissonClient) {
        this.webClient = webClient;
        this.aiChatLogMapper = aiChatLogMapper;
        this.redisTemplate = redisTemplate;
        this.asyncExecutor = asyncExecutor;
        this.redissonClient = redissonClient;
    }

    /**
     * @description: 调用FastGPT接口进行流式问答，并自动记录对话日志
     * @author: Leung Chiu Wai
     * @date: 2025-07-15 10:35:12
     * @param: query 用户输入的提问内容
     * @param: userId 发起提问的用户唯一标识
     * @return: Flux<JSONObject> 包含模型分段回复的响应流
     */
    @Override
    public Flux<JSONObject> chatStreamWithFastGPT(String query, Long userId) {
        // 为了防止接口被恶意冲刷，所以做限制
        // 单个用户每分钟最多允许对话 10 次
        if (userId != null) {
            String limitKey = "rate_limit:ai_chat:" + userId;
            Long count = redisTemplate.opsForValue().increment(limitKey);
            if (count != null && count == 1) {
                redisTemplate.expire(limitKey, Duration.ofMinutes(1));
            }
            if (count != null && count > 10) {
                JSONObject error = new JSONObject();
                error.put("error", "您发送消息过于频繁，请稍后再试！");
                return Flux.just(error);
            }
        }

        // 组装调用大模型所需的标准格式参数体（兼容 OpenAI 消息格式规范）
        JSONObject body = new JSONObject();
        body.put("model", "fastgpt");
        body.put("stream", true); // 开启流式响应，提升客户端的首字渲染速度，改善用户体验
        body.put("messages", new JSONObject[]{
                new JSONObject(){{ put("role", "user"); put("content", query); }}
        });

        // 用于在内存中拼接流式返回的所有字符片段，以便最终能够存入数据库留存对话记录
        StringBuilder fullResponse = new StringBuilder();

        return webClient.post()
                .uri(fastgptUrl)
                .header("Authorization", "Bearer " + fastgptKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(chunk -> chunk != null && !"[DONE]".equals(chunk.trim()))
                .map(chunk -> {
                    String content = "";
                    try {
                        JSONObject json = JSONObject.parseObject(chunk);
                        JSONArray choices = json.getJSONArray("choices");
                        if (choices != null && !choices.isEmpty()) {
                            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                            if (delta != null && delta.containsKey("content")) {
                                content = delta.getString("content");
                            }
                        }
                    } catch (Exception e) {
                        // 忽略解析异常
                    }
                    return content;
                })
                .filter(content -> content != null && !content.isEmpty())
                .doOnNext(content -> fullResponse.append(content))
                .map(content -> {
                    JSONObject result = new JSONObject();
                    result.put("content", content);
                    return result;
                })
                .doFinally(signalType -> {
                    if (fullResponse.length() > 0) {
                        // 核心改造：将耗时的 JDBC 落盘操作丢入异步线程池
                        // 这样 Netty 回调线程可以瞬间执行完毕并被释放去处理下一个用户的请求
                        asyncExecutor.execute(() -> {
                            AiChatLog log = new AiChatLog();
                            log.setUserId(userId != null ? userId : 0L);
                            log.setQuery(query);
                            log.setResponse(fullResponse.toString());
                            try {
                                aiChatLogMapper.insert(log);
                            } catch (Exception e) {
                                // 生产环境绝不能用 printStackTrace，改用 log 记录
                                log.setResponse(e.getMessage());
                            }
                        });
                    }
                })
                .onErrorResume(e -> {
                    JSONObject error = new JSONObject();
                    error.put("error", "FastGPT调用失败: " + e.getMessage());
                    return Flux.just(error);
                });
    }

    /**
     * @description: 调用火山引擎API对长文本内容进行智能语法与拼写校对
     * @author: Leung Chiu Wai
     * @date: 2025-08-01 14:15:22
     * @param: content 待校对的原始公文或草稿文本
     * @return: Mono<Result<String>> 异步封装的校对结果对象
     */
    @Override
    public Mono<Result<String>> proofread(String content) {
        String md5Hash = DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
        String cacheKey = "ai_proofread:" + md5Hash;
        
//        Object cachedResult = redisTemplate.opsForValue().get(cacheKey);
        org.redisson.api.RBucket<Result<String>> bucket = redissonClient.getBucket(cacheKey);


        // 1. 读取缓存
        Result<String> cachedResult = bucket.get();
        if (cachedResult != null) {
            return Mono.just(cachedResult);
        }

        // 构建请求体，火山引擎所需的入参格式较为简明，仅需传入 text 即可
        JSONObject body = new JSONObject();
        body.put("text", content);

        // 采用 Spring WebFlux 提供的非阻塞 WebClient 发起异步 HTTP POST 请求
        return webClient.post()
                .uri(volcengineUrl)
                .header("Authorization", "Bearer " + volcengineKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(Result::success)
                .doOnNext(result -> {
                    // 调用成功后，异步存入 Redis 缓存，保留 7 天
                    if (result.getCode() == 200) {
                        redisTemplate.opsForValue().set(cacheKey, result, Duration.ofDays(7));
                    }
                })
                .onErrorResume(e -> Mono.just(Result.error(500, "火山引擎校对接口调用失败: " + e.getMessage())));
    }
}
