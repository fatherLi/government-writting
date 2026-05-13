package com.myapp.governmentwritting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("real_time_data")
public class RealTimeData {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;   // 数据内容
    private String deviceId;  // 设备/用户ID
    private LocalDateTime createTime;
}