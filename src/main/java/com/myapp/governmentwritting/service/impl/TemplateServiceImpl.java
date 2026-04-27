package com.myapp.governmentwritting.service.impl;

import com.myapp.governmentwritting.entity.DocumentTemplate;
import com.myapp.governmentwritting.mapper.DocumentTemplateMapper;
import com.myapp.governmentwritting.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @description: Template服务实现类，提供具体业务逻辑
 * @author: Leung Chiu Wai
 * @date: 2025-08-03 14:09:09
 * @version: 1.0
 */
@Slf4j
@Service
public class TemplateServiceImpl implements TemplateService {

    @Autowired
    private DocumentTemplateMapper templateMapper;

    @Override
    @CacheEvict(value = "templates", allEntries = true) // 发生上传时，清空模板缓存保证一致性
        /**
     * @description: 执行uploadTemplate相关业务逻辑
     * @author: Leung Chiu Wai
     * @date: 2025-06-14 09:00:26
     * @param: file
     * @param: title
     * @param: createBy
     * @return: void
     */
    public void uploadTemplate(MultipartFile file, String title, Long createBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }
        
        try {
            log.info("开始处理模板上传：标题=[{}], 上传者ID=[{}]", title, createBy);
            
            // 实际应上传到OSS或本地磁盘，这里由于项目简化仅保存模拟的静态资源URL
            String fileUrl = "http://localhost:8080/templates/" + file.getOriginalFilename();
            
            DocumentTemplate template = new DocumentTemplate();
            template.setTitle(title);
            template.setFileUrl(fileUrl);
            template.setCreateTime(LocalDateTime.now());
            template.setCreateBy(createBy);
            
            templateMapper.insert(template);
            log.info("模板上传成功，已入库：{}", title);
        } catch (Exception e) {
            log.error("模板上传持久化异常，标题：{}", title, e);
            throw new RuntimeException("模板上传处理失败", e);
        }
    }

    @Override
    @Cacheable(value = "templates", key = "'all'") // 从Redis中读取缓存，没有则查库并自动写入Redis
        /**
     * @description: 执行listTemplates相关业务逻辑
     * @author: Leung Chiu Wai
     * @date: 2025-07-30 14:46:44
     * @return: List<DocumentTemplate>
     */
    public List<DocumentTemplate> listTemplates() {
        try {
            log.info("拉取全局公文模板列表");
            return templateMapper.selectList(null);
        } catch (Exception e) {
            log.error("查询全量模板列表数据库异常", e);
            throw new RuntimeException("查询模板列表失败", e);
        }
    }
}
