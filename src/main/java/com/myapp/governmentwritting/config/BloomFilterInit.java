package com.myapp.governmentwritting.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myapp.governmentwritting.entity.Document;
import com.myapp.governmentwritting.mapper.DocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;






/**
 **
 * @description: 系统启动时预热布隆过滤器
 * @author: Leung Chiu Wai
 * @date: 2025-08-28 11:51:05
 * @version: 1.0
 */

@Slf4j
@Component
public class BloomFilterInit implements ApplicationRunner {

    private final RedissonClient redissonClient;
    private final DocumentMapper documentMapper;

    public BloomFilterInit(RedissonClient redissonClient, DocumentMapper documentMapper) {
        this.redissonClient = redissonClient;
        this.documentMapper = documentMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 策略 1：异步执行！绝不阻塞 Spring Boot 启动主线程
        CompletableFuture.runAsync(() -> {
            log.info("【后台任务】开始异步检查并预热公文 ID 布隆过滤器...");

            RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("doc_id_bloom_filter");
            // 初始化：预计 100万 条数据，允许 0.01% 的误判率 (按实际业务量修改)
            bloomFilter.tryInit(1000000L, 0.0001);

            // 如果布隆过滤器已经有数据了，说明以前启动时加载过，或者其他节点加载了，直接跳过
            if (bloomFilter.count() > 0) {
                log.info("布隆过滤器已有数据，跳过预热过程。当前容量: {}", bloomFilter.count());
                return;
            }

            // 策略 2：加分布式锁！防止集群下多个实例同时启动，发生“抢跑”重复查询数据库
            RLock initLock = redissonClient.getLock("lock:bloom_filter_init");
            boolean isLocked = false;
            try {
                // 尝试拿锁，最多等 0 秒（拿不到直接走人），拿到后锁住 10 分钟（防止意外死锁）
                isLocked = initLock.tryLock(0, 10, TimeUnit.MINUTES);
                if (isLocked) {
                    log.info("本节点获取到初始化锁，开始从 PostgreSQL 增量加载数据...");
                    loadDataInBatches(bloomFilter);
                } else {
                    log.info("其他节点正在进行初始化，本节点跳过...");
                }
            } catch (InterruptedException e) {
                log.error("尝试获取预热锁时发生中断异常", e);
                Thread.currentThread().interrupt();
            } finally {
                // 释放锁
                if (isLocked && initLock.isHeldByCurrentThread()) {
                    initLock.unlock();
                    log.info("本节点预热完成，释放初始化锁。");
                }
            }
        });
    }

    /**
     * 策略 3：游标滚动查询 (Seek Pagination)
     * 为什么不用传统的 Page 分页？因为 PostgreSQL/MySQL 在深度分页 (Offset > 100万) 时极慢。
     * 每次记住上一批的最后一个 ID，下一批查 id > lastId，利用 B+ 树索引，速度永远保持在毫秒级。
     */
    private void loadDataInBatches(RBloomFilter<Long> bloomFilter) {
        long lastId = 0L;
        int batchSize = 10000; // 每次从数据库捞 1万条
        long totalLoaded = 0L;

        while (true) {
            // 构造条件：查出 ID 大于上一次末尾 ID 的记录，按 ID 升序，限制 1万条
            LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(Document::getId) // 【核心优化】：绝对不要 select *，只要 ID 列！
                    .gt(Document::getId, lastId)
                    .orderByAsc(Document::getId)
                    .last("LIMIT " + batchSize); // 直接拼接 LIMIT 极其高效

            // 查出这 1万个 ID
            List<Document> docs = documentMapper.selectList(wrapper);

            if (docs == null || docs.isEmpty()) {
                break; // 查不到数据了，说明到底了，结束循环
            }

            // 批量将这 1万个 ID 加入布隆过滤器
            for (Document doc : docs) {
                bloomFilter.add(doc.getId());
                lastId = doc.getId(); // 记住最后一个 ID，供下一轮使用
            }

            totalLoaded += docs.size();
            log.info("预热进度：已加载 {} 条数据...", totalLoaded);

            // 为了防止狂占 CPU 和数据库 IO，稍微歇一歇（可选）
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("🎉 布隆过滤器预热大功告成！总计加载合法 ID 数: {}", totalLoaded);
    }
}