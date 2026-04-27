package com.myapp.governmentwritting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: User实体类，对应数据库表结构
 * @author: Leung Chiu Wai
 * @date: 2025-07-24 08:13:47
 * @version: 1.0
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String role; // ADMIN or USER
    private LocalDateTime createTime;
}
