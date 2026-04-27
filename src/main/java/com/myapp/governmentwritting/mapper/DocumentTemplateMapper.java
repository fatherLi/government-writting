package com.myapp.governmentwritting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myapp.governmentwritting.entity.DocumentTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description: DocumentTemplate数据访问层，映射数据库操作
 * @author: Leung Chiu Wai
 * @date: 2025-07-31 14:17:17
 * @version: 1.0
 */
@Mapper
public interface DocumentTemplateMapper extends BaseMapper<DocumentTemplate> {
}
