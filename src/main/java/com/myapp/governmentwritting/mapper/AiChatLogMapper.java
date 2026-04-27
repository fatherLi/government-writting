package com.myapp.governmentwritting.mapper;

import com.myapp.governmentwritting.entity.AiChatLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * @description: AiChatLog数据访问层，映射数据库操作
 * @author: Leung Chiu Wai
 * @date: 2025-07-06 12:15:39
 * @version: 1.0
 */
@Mapper
public interface AiChatLogMapper {

    @Insert("INSERT INTO ai_chat_log(user_id, query, response, create_time) " +
            "VALUES(#{userId}, #{query}, #{response}, CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiChatLog log);
}
