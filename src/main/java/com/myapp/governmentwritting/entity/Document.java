package com.myapp.governmentwritting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: Document实体类，对应数据库表结构
 * @author: Leung Chiu Wai
 * @date: 2025-09-03 12:56:57
 * @version: 1.0
 */
@Data
@TableName("document")
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String fileUrl;
    private Integer status; // 0: draft, 1: published
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
