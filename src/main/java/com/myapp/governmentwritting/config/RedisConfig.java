package com.myapp.governmentwritting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;


/**
 * @description: 企业级 Redis 配置类
 * @author: Leung Chiu Wai
 * @date: 2025-08-28 11:51:05
 * @version: 1.0
 */
@Slf4j
@EnableCaching // 开启Spring Cache注解驱动
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("正在初始化 RedisTemplate...");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // 使用 String 序列化方式，序列化 KEY
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        
        // 使用 JSON 序列化方式（库自带的 Jackson 序列化器），序列化 VALUE
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    // 企业级 RedisCacheManager 配置，统一处理 @Cacheable 的 TTL 和 JSON 序列化
    /**
     * @description: 配置企业级缓存管理器
     * @author: Leung Chiu Wai
     * @date: 2025-06-22 02:10:26
     * @param: factory Redis连接工厂
     * @return: RedisCacheManager 缓存管理器
     */
    @Bean

    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        log.info("正在初始化 RedisCacheManager，默认过期时间：1小时");
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // 默认缓存1小时
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}
