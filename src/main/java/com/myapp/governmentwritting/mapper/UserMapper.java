package com.myapp.governmentwritting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myapp.governmentwritting.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description: User数据访问层，映射数据库操作
 * @author: Leung Chiu Wai
 * @date: 2025-06-03 07:47:32
 * @version: 1.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
