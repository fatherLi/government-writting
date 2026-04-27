package com.myapp.governmentwritting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: DocumentAccessLog实体类，对应数据库表结构
 * @author: Leung Chiu Wai
 * @date: 2025-08-29 00:53:31
 * @version: 1.0
 */
@Data
@TableName("document_access_log")
public class DocumentAccessLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long documentId;
    private LocalDateTime accessTime;
}
