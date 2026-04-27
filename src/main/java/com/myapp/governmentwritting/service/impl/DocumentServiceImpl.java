package com.myapp.governmentwritting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myapp.governmentwritting.entity.Document;
import com.myapp.governmentwritting.entity.DocumentAccessLog;
import com.myapp.governmentwritting.mapper.DocumentAccessLogMapper;
import com.myapp.governmentwritting.mapper.DocumentMapper;
import com.myapp.governmentwritting.service.DocumentService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * @description: Document服务实现类，提供具体业务逻辑
 * @author: Leung Chiu Wai
 * @date: 2025-09-04 08:57:40
 * @version: 1.0
 */
@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {


    private final DocumentMapper documentMapper;


    private final DocumentAccessLogMapper accessLogMapper;


    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    public DocumentServiceImpl(DocumentMapper documentMapper, DocumentAccessLogMapper accessLogMapper, org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate, org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor asyncExecutor) {
        this.documentMapper = documentMapper;
        this.accessLogMapper = accessLogMapper;
        this.redisTemplate = redisTemplate;
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * @description: 获取MyDocuments属性值
     * @author: Leung Chiu Wai
     * @date: 2025-06-14 04:06:21
     * @param: userId
     * @return: List<Document>
     */
    @Override
    @Cacheable(value = "user_docs", key = "#userId") // 按照用户ID缓存公文列表
    public List<Document> getMyDocuments(Long userId) {
        if (userId == null) {
            log.warn("查询公文列表失败：用户ID为空");
            return Collections.emptyList();
        }
        
        try {
            log.info("查询用户(ID:{})的全部公文列表", userId);
            LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Document::getUserId, userId);
            queryWrapper.orderByDesc(Document::getCreateTime);
            return documentMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("查询用户公文列表发生数据库异常", e);
            throw new RuntimeException("查询公文列表失败", e);
        }
    }

    /**
     * @description: 获取RecentDocuments属性值
     * @author: Leung Chiu Wai
     * @date: 2025-08-18 05:24:19
     * @param: userId
     * @return: List<Document>
     */
    @Override
    public List<Document> getRecentDocuments(Long userId) {
        if (userId == null) {
            log.warn("查询最近公文失败：用户ID为空");
            return Collections.emptyList();
        }
        
        try {
            String key = "recent_docs_zset:" + userId;
            // 1. 从 Redis ZSet 中获取分数（时间戳）最高的前 10 个 documentId
            java.util.Set<Object> docIds = redisTemplate.opsForZSet().reverseRange(key, 0, 9);
            
            if (docIds == null || docIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> documentIds = docIds.stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(Collectors.toList());
                    
            // 2. 批量查库获取详细信息
            List<Document> docs = documentMapper.selectBatchIds(documentIds);
            
            // 3. 由于查库结果可能是乱序的，按照 ZSet 中最新的顺序重新排序返回
            return documentIds.stream()
                    .map(id -> docs.stream().filter(d -> d.getId().equals(id)).findFirst().orElse(null))
                    .filter(d -> d != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询用户(ID:{})最近访问公文列表异常", userId, e);
            return Collections.emptyList(); // 降级处理，不影响主流程
        }
    }


    @org.springframework.beans.factory.annotation.Qualifier("asyncExecutor")
    private final org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor asyncExecutor;

    /**
     * @description: 新增数据记录
     * @author: Leung Chiu Wai
     * @date: 2025-09-09 02:18:30
     * @param: userId
     * @param: documentId
     * @return: void
     */
    @Override
    public void addRecentDocument(Long userId, Long documentId) {
        if (userId == null || documentId == null) {
            log.warn("添加最近浏览记录失败：参数缺失");
            return;
        }

        try {
            String key = "recent_docs_zset:" + userId;
            long currentTime = System.currentTimeMillis();
            
            // 1. 将当前访问记录以当前时间戳作为分数，存入 ZSet
            redisTemplate.opsForZSet().add(key, documentId.toString(), currentTime);
            
            // 2. 优化：限制 ZSet 大小，只保留最近的 50 条记录
            redisTemplate.opsForZSet().removeRange(key, 0, -51);
            
            // 3. 将极其耗时的数据库 insert 操作丢入专门的 IO 线程池异步执行，主线程直接返回！
            asyncExecutor.execute(() -> {
                try {
                    DocumentAccessLog logEntry = new DocumentAccessLog();
                    logEntry.setUserId(userId);
                    logEntry.setDocumentId(documentId);
                    accessLogMapper.insert(logEntry);
                } catch (Exception e) {
                    // 打印异常，绝不能影响主业务逻辑
                    log.error("异步记录公文访问日志(用户ID:{}, 文档ID:{})失败", userId, documentId, e);
                }
            });
        } catch (Exception e) {
            log.error("添加最近浏览缓存记录(用户ID:{}, 文档ID:{})发生异常", userId, documentId, e);
        }
    }

    /**
     * @description: 获取DocumentById属性值
     * @author: Leung Chiu Wai
     * @date: 2025-06-13 15:31:31
     * @param: documentId
     * @param: userId
     * @return: Document
     */
    @Override
    public Document getDocumentById(Long documentId, Long userId) {
        if (documentId == null || userId == null) {
            log.warn("获取公文详情失败：参数缺失");
            return null;
        }
        
        try {
            Document document = documentMapper.selectById(documentId);
            if (document != null && document.getUserId().equals(userId)) {
                // 企业级闭环：在用户成功获取到文章详情时，自动将其加入该用户的“最近浏览” Redis ZSet中
                addRecentDocument(userId, documentId);
                return document;
            } else {
                log.warn("未找到该公文或该用户(ID:{})无权访问公文(ID:{})", userId, documentId);
                return null;
            }
        } catch (Exception e) {
            log.error("获取公文详情(文档ID:{})异常", documentId, e);
            throw new RuntimeException("获取公文详情失败", e);
        }
    }
}
