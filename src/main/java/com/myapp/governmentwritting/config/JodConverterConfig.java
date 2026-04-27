package com.myapp.governmentwritting.config;

import lombok.extern.slf4j.Slf4j;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.io.File;

/**
 * @description: JodConverter配置类，用于项目配置。
 * 已增强健壮性：即使本地未安装 LibreOffice，项目也能正常启动。
 * @author: Leung Chiu Wai
 * @date: 2025-06-30 19:28:56
 * @version: 1.1
 */
@Slf4j
@Configuration
public class JodConverterConfig {

    @Value("${jodconverter.local.enabled:true}")
    private boolean enabled;

    @Value("${jodconverter.local.office-home:}")
    private String officeHome;

    @Value("${jodconverter.local.port-numbers:2002}")
    private int[] portNumbers;

    @Value("${jodconverter.local.max-tasks-per-process:10}")
    private int maxTasksPerProcess;

    private OfficeManager officeManager;

    /**
     * @description: 初始化文档转换器。若环境未安装 LibreOffice，将捕获异常并返回 null，确保应用主进程不崩溃。
     * @return: DocumentConverter 文档转换实例，若初始化失败则返回 null
     */
    @Bean
    public DocumentConverter documentConverter() {
        if (!enabled) {
            log.info("JodConverter 功能已根据配置禁用。");
            return null;
        }

        log.info("正在尝试初始化 LibreOffice 服务管理器...");
        try {
            LocalOfficeManager.Builder builder = LocalOfficeManager.builder()
                    .portNumbers(portNumbers)
                    .maxTasksPerProcess(maxTasksPerProcess);

            // 预检查 officeHome
            if (officeHome != null && !officeHome.trim().isEmpty()) {
                File homeFile = new File(officeHome);
                if (homeFile.exists()) {
                    log.info("使用指定的 OfficeHome: {}", officeHome);
                    builder.officeHome(officeHome);
                } else {
                    log.warn("配置的 office-home 路径不存在: {}，将尝试使用系统默认路径。", officeHome);
                }
            }

            officeManager = builder.build();
            officeManager.start();
            log.info("LibreOffice 服务管理器启动成功。");
            return LocalConverter.make(officeManager);
        } catch (Throwable t) {
            // 关键点：捕获所有 Throwable（包括 Error 和 Exception），防止启动中断
            log.error("LibreOffice/OpenOffice 服务管理器启动失败！原因: {}. " +
                    "提示：若本地未安装 LibreOffice，此属正常现象。项目将继续启动，但 Word 转 PDF 功能将报'未安装'错误。", t.getMessage());
            officeManager = null;
            return null;
        }
    }

    /**
     * @description: 系统关闭前优雅停止 Office 服务管理器
     */
    @PreDestroy
    public void destroy() {
        if (officeManager != null) {
            try {
                if (officeManager.isRunning()) {
                    log.info("正在停止 LibreOffice 服务管理器...");
                    officeManager.stop();
                    log.info("LibreOffice 服务管理器已成功停止。");
                }
            } catch (Exception e) {
                log.error("停止 LibreOffice 服务管理器时发生异常: ", e);
            }
        }
    }
}

