package com.swj.shiwujie.chatmemory;

import com.swj.shiwujie.model.domain.ai.AiLogs;
import com.swj.shiwujie.service.AiLogsService;
import com.swj.shiwujie.utils.LightweightMessageSerializer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MySQL聊天记录存储器
 * 用于将聊天记录异步存储到MySQL数据库中
 */
@Component
@Slf4j
public class MySQLChatMemoryStore {

    @Resource
    private AiLogsService aiLogsService;

    /**
     * 异步将消息存储到MySQL数据库
     * @param conversationId 会话ID（用户ID）
     * @param messages 消息列表
     */
    @Async
    public void storeMessages(String conversationId, List<Message> messages) {
        try {
            if (messages == null || messages.isEmpty()) {
                return;
            }

            Long userId = Long.parseLong(conversationId);
            for (Message message : messages) {
                AiLogs logEntry = new AiLogs();
                logEntry.setOperatorId(userId);
                logEntry.setContent(LightweightMessageSerializer.serialize(message));
                aiLogsService.save(logEntry);
            }
            
            log.debug("成功将 {} 条消息存储到MySQL数据库，会话ID: {}", messages.size(), conversationId);
        } catch (Exception e) {
            log.error("将消息存储到MySQL数据库时发生错误，会话ID: {}", conversationId, e);
        }
    }
}
