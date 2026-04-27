package com.myapp.governmentwritting.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * @description: AiChatLog实体类，对应数据库表结构
 * @author: Leung Chiu Wai
 * @date: 2025-06-23 00:51:17
 * @version: 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatLog {
    private Long id;
    private Long userId;
    private String query;
    private String response;
    private LocalDateTime createTime;
}
