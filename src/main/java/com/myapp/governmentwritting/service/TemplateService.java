package com.myapp.governmentwritting.service;

import com.myapp.governmentwritting.entity.DocumentTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * @description: Template服务接口，定义业务方法
 * @author: Leung Chiu Wai
 * @date: 2025-09-07 08:08:37
 * @version: 1.0
 */
public interface TemplateService {
    void uploadTemplate(MultipartFile file, String title, Long createBy);
    List<DocumentTemplate> listTemplates();
}
