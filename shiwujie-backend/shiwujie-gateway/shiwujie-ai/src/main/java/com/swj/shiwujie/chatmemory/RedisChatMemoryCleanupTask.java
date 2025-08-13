package com.swj.shiwujie.chatmemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis对话历史清理定时任务
 * 每天凌晨执行，清理超过20条消息的对话记录
 */
@Component
@Slf4j
public class RedisChatMemoryCleanupTask {

    @Autowired
    private RedisChatMemory redisChatMemory;

    /**
     * 每天凌晨2点执行清理任务
     * 0 0 2 * * ? 表示：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldConversations() {
        log.info("开始执行Redis对话历史清理任务");
        try {
            List<String> conversationIds = redisChatMemory.getAllConversationIds();
            int trimmedCount = 0;
            
            for (String conversationId : conversationIds) {
                try {
                    redisChatMemory.trimConversation(conversationId);
                    trimmedCount++;
                } catch (Exception e) {
                    log.error("清理对话 [{}] 时发生错误", conversationId, e);
                }
            }
            
            log.info("Redis对话历史清理任务完成，共处理 {} 个对话", trimmedCount);
        } catch (Exception e) {
            log.error("执行Redis对话历史清理任务时发生错误", e);
        }
    }
}
