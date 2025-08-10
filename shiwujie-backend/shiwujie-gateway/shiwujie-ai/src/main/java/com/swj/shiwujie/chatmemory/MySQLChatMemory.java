package com.swj.shiwujie.chatmemory;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.mapper.AiLogsMapper;
import com.swj.shiwujie.model.domain.ai.AiLogs;
import com.swj.shiwujie.utils.LightweightMessageSerializer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


/**
 * 自定义数据库持久化
 */
@Service
@Slf4j
public class MySQLChatMemory implements ChatMemory {


    @Resource
    private AiLogsMapper aiLogsMapper;

    /**
     * 添加多条数据到数据库中
     * @param conversationId
     * @param messages
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        // 保存所有消息
        Long userId = Long.parseLong(conversationId); // 直接解析 userId
        List<AiLogs> logsList = new ArrayList<>();
        for (Message message : messages) {
            AiLogs logEntry = new AiLogs();
            logEntry.setOperatorId(userId);
            logEntry.setContent(LightweightMessageSerializer.serialize(message));
            logsList.add(logEntry);
        }
        
        if (!logsList.isEmpty()) {
            // 批量插入消息
            for (AiLogs logEntry : logsList) {
                aiLogsMapper.insert(logEntry);
            }
        }
        
        log.debug("已向对话 [{}] 添加 {} 条消息", conversationId, messages.size());
    }

    /**
     * 从数据库中获取数据
     * 从数据库中获取倒数lastN条数据
     * @param conversationId
     * @param lastN
     * @return
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        Long userId = Long.parseLong(conversationId); // 直接解析 userId
        QueryWrapper<AiLogs> wrapper = new QueryWrapper<>();
        wrapper.eq("operator_id", userId) // 使用解析出的 userId 过滤
                .orderByAsc("create_time"); // 按创建时间正序排列

        List<AiLogs> logsList = aiLogsMapper.selectList(wrapper);
        
        // 取最后 N 条消息
        int fromIndex = Math.max(0, logsList.size() - lastN);
        List<AiLogs> subList = logsList.subList(fromIndex, logsList.size());

        List<Message> messages = new ArrayList<>();
        for (AiLogs logEntry : subList) {
            try {
                Message message = LightweightMessageSerializer.deserialize(logEntry.getContent());
                messages.add(message);
            } catch (Exception e) {
                log.error("反序列化消息失败，跳过该消息: {}", logEntry.getContent(), e);
            }
        }
        return messages;
    }

    /**
     * 清空数据
     * @param conversationId
     */
    @Override
    public void clear(String conversationId) {
        Long userId = Long.parseLong(conversationId); // 直接解析 userId
        QueryWrapper<AiLogs> logQueryWrapper = new QueryWrapper<>();
        logQueryWrapper.eq("operator_id", userId);
        aiLogsMapper.delete(logQueryWrapper);
        log.debug("已清空对话 [{}] 的历史消息", conversationId);
    }
}


