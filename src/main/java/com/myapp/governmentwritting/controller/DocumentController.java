package com.myapp.governmentwritting.controller;

import com.myapp.governmentwritting.common.Result;
import com.myapp.governmentwritting.common.SecurityUtils;
import com.myapp.governmentwritting.entity.Document;
import com.myapp.governmentwritting.service.DocumentService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @description: 公文文档核心控制器，负责处理用户对公文的增删改查及最近访问记录查询等前端请求
 * @author: Leung Chiu Wai
 * @date: 2025-06-25 10:20:00
 * @version: 1.0
 */
@RestController
@RequestMapping("/api/document")
public class DocumentController {


    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // 获取我所有的公文
    /**
     * @description: 获取当前登录用户创建的所有公文列表
     * @author: Leung Chiu Wai
     * @date: 2025-07-26 22:05:59
     * @return: Result<List<Document>> 统一响应体，包含公文实体列表
     */
    @GetMapping("/my/all")
    public Result<List<Document>> getMyDocuments() {
        // 从当前线程的安全上下文中提取用户ID，确保数据隔离机制的执行
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) return Result.error(401, "未登录");
        List<Document> documents = documentService.getMyDocuments(currentUserId);
        return Result.success(documents);
    }

    // 获取最近打开访问的10条公文
    /**
     * @description: 获取当前登录用户最近打开/访问过的最多10条公文记录
     * @author: Leung Chiu Wai
     * @date: 2025-08-03 22:55:13
     * @return: Result<List<Document>> 统一响应体，包含最近访问的公文实体列表
     */
    @GetMapping("/my/recent")
    public Result<List<Document>> getRecentDocuments() {
        // 鉴权拦截：所有业务接口必须依赖有效登录态，若未获取到身份凭证直接拒绝访问
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) return Result.error(401, "未登录");
        List<Document> recentDocs = documentService.getRecentDocuments(currentUserId);
        return Result.success(recentDocs);
    }

    // 获取公文详情（企业级闭环：用户调用此接口阅读时，底层自动写入 Redis ZSet 浏览记录）
    /**
     * @description: 根据公文ID查询公文详情，并在底层自动触发阅读记录的存储行为
     * @author: Leung Chiu Wai
     * @date: 2025-09-16 15:08:08
     * @param: id 公文的唯一主键ID
     * @return: Result<Document> 统一响应体，包含查询到的具体公文信息
     */
    @GetMapping("/{id}")
    public Result<Document> getDocumentById(@org.springframework.web.bind.annotation.PathVariable Long id) {
        // 核心流程：先进行用户身份验证，通过后再请求底层数据引擎拉取明细
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) return Result.error(401, "未登录");
        
        Document document = documentService.getDocumentById(id, currentUserId);
        if (document == null) return Result.error(404, "公文不存在或无权访问");
        return Result.success(document);
    }
}
