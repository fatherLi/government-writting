package com.myapp.governmentwritting.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.myapp.governmentwritting.service.FileConvertService;
import org.jodconverter.core.DocumentConverter;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * @description: 文件格式转换服务实现类，利用JodConverter实现本地或远端的Word文档转PDF功能
 * @author: Leung Chiu Wai
 * @date: 2025-08-20 09:12:44
 * @version: 1.0
 */
@Service
public class FileConvertServiceImpl implements FileConvertService {


    private final DocumentConverter documentConverter;


    @org.springframework.beans.factory.annotation.Qualifier("heavyCpuExecutor")
    private final org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor heavyCpuExecutor;

    @org.springframework.beans.factory.annotation.Autowired
    public FileConvertServiceImpl(@org.springframework.beans.factory.annotation.Autowired(required = false) DocumentConverter documentConverter, 
                                  @org.springframework.beans.factory.annotation.Qualifier("heavyCpuExecutor") ThreadPoolTaskExecutor heavyCpuExecutor) {
        this.documentConverter = documentConverter;
        this.heavyCpuExecutor = heavyCpuExecutor;
    }

    /**
     * @description: 接收远程Word文档链接，将其下载并转换为PDF，最后以二进制流的形式写入到HttpServletResponse中供用户直接预览
     * @author: Leung Chiu Wai
     * @date: 2025-08-20 09:15:33
     * @param: wordUrl 远端Word文件的下载地址（大模型返回或OSS地址）
     * @param: response 当前请求的响应对象，用于直接输出PDF文件流
     * @return: void 此方法直接操作响应流，不返回数据实体
     */
    @Override
    public void convertWordToPdf(String wordUrl, HttpServletResponse response) throws Exception {
        // 核心容错处理：由于 LibreOffice 属于重型外部依赖，可能因环境问题未安装。
        // 此处做拦截处理，若底层转换器实例未装载，直接快速失败，避免后续出现 NullPointerException 导致不可控崩溃。
        if (documentConverter == null) {
            throw new RuntimeException("系统未安装或未正确配置 LibreOffice，无法执行 Word 转 PDF 操作！");
        }

        // 1. 生成临时文件路径：利用系统的临时目录和随机UUID确保并发环境下各请求的临时文件不会互相覆盖
        String tempDir = System.getProperty("java.io.tmpdir");
        String wordFileName = UUID.randomUUID().toString() + ".docx";
        String pdfFileName = UUID.randomUUID().toString() + ".pdf";

        File wordFile = new File(tempDir, wordFileName);
        File pdfFile = new File(tempDir, pdfFileName);

        try {
            // 2. 从URL下载Word文件到本地临时文件区
            HttpUtil.downloadFile(wordUrl, wordFile);

            // 防止在突发高并发转换请求时把 Tomcat 主工作线程池打满，从而导致整个系统拒绝服务(OOM或卡死)。
            java.util.concurrent.CompletableFuture<Void> convertFuture = java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    documentConverter.convert(wordFile).to(pdfFile).execute();
                } catch (Exception e) {
                    throw new RuntimeException("底层 LibreOffice 文件格式转换异常", e);
                }
            }, heavyCpuExecutor);
            
            // 阻塞主线程等待后台转换完成（这里利用线程池的有界队列与最大线程数机制自动起到了削峰填谷的作用）
            convertFuture.join();

            // 4. 将生成的PDF以流的形式写入 Response，配置 Content-Disposition 为 inline 以支持浏览器原生 PDF 预览
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=preview.pdf");
            
            try (FileInputStream fis = new FileInputStream(pdfFile);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }
        } finally {
            // 避免因磁盘垃圾文件不断积累最终导致服务器存储空间打满
            FileUtil.del(wordFile);
            FileUtil.del(pdfFile);
        }
    }
}
