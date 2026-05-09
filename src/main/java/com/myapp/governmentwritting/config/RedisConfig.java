package com.myapp.governmentwritting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.time.Duration;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;


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


    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        // 1. 集群配置
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(
                redisProperties.getCluster().getNodes());
        clusterConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
        clusterConfig.setMaxRedirects(redisProperties.getCluster().getMaxRedirects());

        // 2. 开启自适应拓扑刷新（重点：解决节点切换连接失败问题）
        ClusterTopologyRefreshOptions refreshOptions = ClusterTopologyRefreshOptions.builder()
                .enableAdaptiveRefreshTrigger(
                        ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT,
                        ClusterTopologyRefreshOptions.RefreshTrigger.PERSISTENT_RECONNECTS)
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(30))
                .build();

        ClusterClientOptions clientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(refreshOptions)
                .build();

        // 3. 配置 Lettuce 客户端
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(getPoolConfig(redisProperties.getLettuce().getPool()))
                .clientOptions(clientOptions)
                .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }



    private GenericObjectPoolConfig getPoolConfig(RedisProperties.Pool pool) {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(pool.getMaxActive());
        config.setMaxIdle(pool.getMaxIdle());
        config.setMinIdle(pool.getMinIdle());
        return config;
    }
}
