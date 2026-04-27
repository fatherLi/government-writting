package com.myapp.governmentwritting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @description: ThreadPool配置类，用于项目配置
 * @author: Leung Chiu Wai
 * @date: 2025-07-14 05:59:18
 * @version: 1.0
 */
@Configuration
@EnableAsync // 开启 Spring 异步注解支持
public class ThreadPoolConfig {

    public static final String ASYNC_EXECUTOR = "asyncExecutor";
    public static final String HEAVY_CPU_EXECUTOR = "heavyCpuExecutor";

    /**
     * 1. 核心业务异步线程池 (IO 密集型，用于数据库落盘等)
     * 企业级真实考量：
     * 虽然八股文常背 2N，但在真实混合部署（还有DB、Redis等其他服务）的环境下，
     * 分配 32 个核心线程给单独一个服务的非核心异步业务是极其“野蛮”的。
     * 由于只是做极快响应的异步 Insert 操作，4 个核心线程足以支撑上千 QPS 的落盘吞吐量，
     * 剩下的请求放在较大的队列里排队即可，不与主业务抢夺 CPU 时间片。
     */
    /**
     * @description: 构建用于一般业务异步处理（如：日志落盘）的轻量级 IO 线程池
     * @author: Leung Chiu Wai
     * @date: 2025-07-20 03:50:56
     * @return: ThreadPoolTaskExecutor Spring封装的异步任务执行器
     */
    @Bean(ASYNC_EXECUTOR)
    public ThreadPoolTaskExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：设置为 4，极其克制，绝不拖垮宿主机其他服务
        executor.setCorePoolSize(4);
        // 最大线程数：8，作为防线
        executor.setMaxPoolSize(8);
        // 队列大小：放大到 1000。并发200打进来全部进队列，4个线程慢慢消费，波澜不惊
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("Async-IO-");
        // 队列满后的拒绝策略：由提交任务的线程自己去执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 2. 重度外部计算线程池 (专门用于 LibreOffice 转换)
     * 企业级真实考量：
     * LibreOffice 转换不是普通的 Java 线程内计算，它会在 OS 层面 fork 外部进程 (soffice.bin)！
     * 如果放任开启 16 个甚至 32 个转换线程，不仅瞬间吃掉几个 G 的内存，而且 16 个进程同时抢夺 8 个 CPU 核心，
     * 必定导致服务器负载瞬间飙升（Load Average 爆炸），所有服务全部卡死。
     * 因此，必须严格限制最大并发转换数，把压力压在 JVM 的等待队列里。
     */
    /**
     * @description: 构建专门用于密集型 CPU 计算（如 LibreOffice 格式转换）的受限线程池
     * @author: Leung Chiu Wai
     * @date: 2025-08-27 21:40:55
     * @return: ThreadPoolTaskExecutor 严格限流的异步执行器
     */
    @Bean(HEAVY_CPU_EXECUTOR)
    public ThreadPoolTaskExecutor heavyCpuExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：严格控制在 2！只允许 2 个 Word 同时转 PDF
        executor.setCorePoolSize(2);
        // 最大线程数：极限情况最多允许 4 个外部进程同时运行
        executor.setMaxPoolSize(4);
        // 队列：放大到 200。保证 200 个人同时点转换，都不会报错，只是后面的人要多转圈等待十几秒
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("LibreOffice-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
