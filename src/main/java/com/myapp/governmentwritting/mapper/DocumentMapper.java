package com.myapp.governmentwritting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myapp.governmentwritting.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description: Document数据访问层，映射数据库操作
 * @author: Leung Chiu Wai
 * @date: 2025-07-24 11:07:30
 * @version: 1.0
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
