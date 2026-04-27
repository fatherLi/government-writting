package com.myapp.governmentwritting.controller;

import com.myapp.governmentwritting.common.Result;
import com.myapp.governmentwritting.common.SecurityUtils;
import com.myapp.governmentwritting.entity.DocumentTemplate;
import com.myapp.governmentwritting.service.TemplateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @description: Template控制器，处理相关HTTP请求
 * @author: Leung Chiu Wai
 * @date: 2025-08-31 13:08:49
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/template")
public class TemplateController {


    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    // 只有管理员可以上传模板
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload")
    public Result<String> uploadTemplate(@RequestParam("file") MultipartFile file, 
                                         @RequestParam("title") String title) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "上传的文件不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            return Result.error(400, "模板标题不能为空");
        }
        
        Long adminId = SecurityUtils.getCurrentUserId();
        if (adminId == null) {
            log.warn("模板上传失败：未获取到当前登录用户ID");
            return Result.error(401, "未登录或登录已失效");
        }
        
        try {
            log.info("管理员(ID:{})开始上传模板: {}, 文件大小: {} bytes", adminId, title, file.getSize());
            templateService.uploadTemplate(file, title, adminId);
            return Result.success("模板上传成功");
        } catch (Exception e) {
            log.error("模板上传过程中发生系统异常", e);
            return Result.error(500, "模板上传失败：" + e.getMessage());
        }
    }

    // 用户可以访问/查询模板列表
    /**
     * @description: 执行listTemplates相关业务逻辑
     * @author: Leung Chiu Wai
     * @date: 2025-06-18 10:28:12
     * @return: Result<List<DocumentTemplate>>
     */
    @GetMapping("/list")
    public Result<List<DocumentTemplate>> listTemplates() {
        try {
            log.info("用户请求获取模板列表");
            List<DocumentTemplate> templates = templateService.listTemplates();
            return Result.success(templates);
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return Result.error(500, "获取模板列表发生内部错误");
        }
    }
}
