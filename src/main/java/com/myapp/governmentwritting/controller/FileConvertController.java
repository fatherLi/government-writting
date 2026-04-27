package com.myapp.governmentwritting.controller;

import com.myapp.governmentwritting.common.Result;
import com.myapp.governmentwritting.service.FileConvertService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * @description: 文件格式转换控制器，负责处理来自前端或大模型的Word转PDF等格式转换请求
 * @author: Leung Chiu Wai
 * @date: 2025-09-03 11:55:25
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileConvertController {


    private final FileConvertService fileConvertService;

    public FileConvertController(FileConvertService fileConvertService) {
        this.fileConvertService = fileConvertService;
    }

    /**
     * @description: 将远程Word文档下载并转换为PDF格式，直接向浏览器输出PDF数据流以供内嵌预览
     * @author: Leung Chiu Wai
     * @date: 2025-09-03 11:55:25
     * @param: wordUrl 远端Word文档的下载直链
     * @param: response HttpServletResponse对象，用于写入PDF二进制流
     * @return: Result<String> 成功时返回null（因直接写流），失败时返回包含错误信息的JSON响应
     */
    @GetMapping("/wordToPdf")
    public Result<String> wordToPdf(@RequestParam("wordUrl") String wordUrl, HttpServletResponse response) {
        if (!StringUtils.hasText(wordUrl)) {
            log.warn("文件转换失败：未提供远端Word下载地址");
            return Result.error(400, "wordUrl不能为空");
        }
        
        try {
            log.info("开始处理远端Word文件转PDF预览，下载地址: {}", wordUrl);
            // 调用底层格式转换服务，服务内部会处理文件下载、LibreOffice转换以及向response写流的完整闭环
            fileConvertService.convertWordToPdf(wordUrl, response);
            // 正常写流完毕后必须返回null，防止Spring MVC的视图解析器或消息转换器对已被写入的流造成破坏
            return null;
        } catch (Exception e) {
            // 异常兜底：若转换过程中出现由于网络、依赖等问题引发的崩溃，捕获并返回标准化错误结构
            log.error("Word转PDF预览过程中发生系统异常, url: {}", wordUrl, e);
            return Result.error(500, "文件转换失败: " + e.getMessage());
        }
    }
}
