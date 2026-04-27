package com.myapp.governmentwritting.config;

import com.myapp.governmentwritting.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @description: GlobalExceptionHandler异常处理机制，负责捕获与处理业务异常
 * @author: Leung Chiu Wai
 * @date: 2025-08-27 01:11:17
 * @version: 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @description: 执行handleException相关业务逻辑
     * @author: Leung Chiu Wai
     * @date: 2025-08-06 15:32:56
     * @param: e
     * @return: Result<String>
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("全局异常捕获：发生未处理的系统异常", e);
        return Result.error(500, "系统繁忙，请稍后再试: " + e.getMessage());
    }
}
