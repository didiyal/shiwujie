package com.swj.shiwujie.chatmemory;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.mapper.AiLogsMapper;
import com.swj.shiwujie.model.domain.ai.AiLogs;
import com.swj.shiwujie.utils.MessageSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 自定义数据库持久化
 */
@Service
@Slf4j
public class MySQLChatMemory implements ChatMemory {


    private final AiLogsMapper aiLogsMapper;

    public MySQLChatMemory(AiLogsMapper mapper){
        this.aiLogsMapper = mapper;
    }
    

            

    /**
     * 添加一个数据到数据库中
     * @param conversationId
     * @param message
     */
    @Override
    public void add(String conversationId, Message message) {
        if (message == null) {
            log.warn("尝试添加空消息到对话: {}", conversationId);
            return;
        }
        add(conversationId, List.of(message));
    }

    /**
     * 添加多条数据到数据库中
     * @param conversationId
     * @param messages
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            log.warn("尝试添加空消息列表到对话: {}", conversationId);
            return;
        }
        
        // 获取现有消息列表
        List<Message> existingMessages = get(conversationId, Integer.MAX_VALUE);
        // 合并消息
        existingMessages.addAll(messages);
        
        // 清除旧记录
        clear(conversationId);
        
        // 保存所有消息
        Long userId = Long.parseLong(conversationId); // 直接解析 userId
        List<AiLogs> logsList = new ArrayList<>();
        for (Message message : existingMessages) {
            AiLogs logEntry = new AiLogs();
            logEntry.setOperatorId(userId);
            logEntry.setContent(MessageSerializer.serialize(message));
            logsList.add(logEntry);
        }
        if (!logsList.isEmpty()) {
            // 批量插入消息
            for (AiLogs logEntry : logsList) {
                aiLogsMapper.insert(logEntry);
            }
        }
        log.debug("已向对话 [{}] 添加 {} 条消息，当前总消息数: {}",
                conversationId, messages.size(), existingMessages.size());
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
        if (lastN <= 0) {
            log.warn("获取对话 [{}] 消息时，lastN 参数必须大于 0: {}", conversationId, lastN);
            return new ArrayList<>();
        }
        
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
                messages.add(MessageSerializer.deserialize(logEntry.getContent()));
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


