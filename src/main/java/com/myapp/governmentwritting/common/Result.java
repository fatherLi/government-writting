package com.myapp.governmentwritting.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @description: 企业级统一响应结果封装类
 * @author: Leung Chiu Wai
 * @date: 2025-07-24 19:46:47
 * @version: 1.0
 */
@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    /**
     * @description: 成功响应（带数据）
     * @author: Leung Chiu Wai
     * @date: 2025-06-29 11:50:58
     * @param: data 响应数据
     * @return: Result<T>
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * @description: 成功响应（不带数据）
     * @author: Leung Chiu Wai
     * @date: 2025-09-24 16:48:21
     * @return: Result<T>
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * @description: 错误响应
     * @author: Leung Chiu Wai
     * @date: 2025-08-05 15:07:40
     * @param: code 错误码
     * @param: message 错误信息
     * @return: Result<T>
     */
    public static <T> Result<T> error(Integer code, String message) {
        log.warn("业务处理异常: code={}, message={}", code, message);
        return new Result<>(code, message, null);
    }
}
