package com.myapp.governmentwritting.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.myapp.governmentwritting.entity.RealTimeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.myapp.governmentwritting.config.ThreadPoolConfig.DATA_EXECUTOR;

@Service
public class DataService {

//    @Autowired
//    private RealTimeDataMapper dataMapper;

    @Async(DATA_EXECUTOR)
    public void asyncSave(String json) {
        try {
            // 解析 JSON 数据
            JSONObject jsonObject = JSON.parseObject(json);

            RealTimeData entity = new RealTimeData();
            entity.setContent(jsonObject.getString("content"));
            entity.setDeviceId(jsonObject.getString("deviceId"));
            entity.setCreateTime(LocalDateTime.now());

            // 执行入库
//            dataMapper.insert(entity);
            System.out.println("数据入库成功: " + entity.getDeviceId());
        } catch (Exception e) {
            System.err.println("数据解析或入库失败: " + e.getMessage());
        }
    }
}