package com.myapp.governmentwritting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: DocumentTemplate实体类，对应数据库表结构
 * @author: Leung Chiu Wai
 * @date: 2025-06-08 11:10:07
 * @version: 1.0
 */
@Data
@TableName("document_template")
public class DocumentTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String fileUrl;
    private Long createBy; // admin user id
    private LocalDateTime createTime;
}
