package com.myapp.governmentwritting.service;

import com.myapp.governmentwritting.entity.Document;
import java.util.List;

/**
 * @description: Document服务接口，定义业务方法
 * @author: Leung Chiu Wai
 * @date: 2025-06-20 16:44:08
 * @version: 1.0
 */
public interface DocumentService {
    List<Document> getMyDocuments(Long userId);
    List<Document> getRecentDocuments(Long userId);
    void addRecentDocument(Long userId, Long documentId);
    Document getDocumentById(Long documentId, Long userId);
}
