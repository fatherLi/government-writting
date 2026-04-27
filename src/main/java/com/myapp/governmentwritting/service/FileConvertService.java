package com.myapp.governmentwritting.service;

import javax.servlet.http.HttpServletResponse;

/**
 * @description: FileConvert服务接口，定义业务方法
 * @author: Leung Chiu Wai
 * @date: 2025-07-01 01:37:00
 * @version: 1.0
 */
public interface FileConvertService {
    void convertWordToPdf(String wordUrl, HttpServletResponse response) throws Exception;
}
