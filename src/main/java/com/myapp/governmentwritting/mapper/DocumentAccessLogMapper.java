package com.myapp.governmentwritting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myapp.governmentwritting.entity.DocumentAccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description: DocumentAccessLog数据访问层，映射数据库操作
 * @author: Leung Chiu Wai
 * @date: 2025-06-20 02:22:52
 * @version: 1.0
 */
@Mapper
public interface DocumentAccessLogMapper extends BaseMapper<DocumentAccessLog> {
}
