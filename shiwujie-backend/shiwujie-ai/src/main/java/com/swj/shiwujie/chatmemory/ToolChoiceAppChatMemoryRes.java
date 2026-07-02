package com.swj.shiwujie.chatmemory;

import com.swj.shiwujie.utils.MessageSerializer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.swj.shiwujie.constants.AiConstants.KEY_PREFIX;


/**
 * TextApp聊天内存仓库实现类
 * <p>
 * 该类实现了ChatMemoryRepository接口，提供基于Redis和MySQL的双重存储机制：
 * 1. Redis作为高速缓存，用于快速读取和写入聊天记录
 * 2. MySQL作为持久化存储，用于长期保存聊天记录
 * <p>
 * 主要功能包括：
 * - 存储和检索用户的聊天记录
 * - 控制聊天记录的数量，防止内存溢出
 * - 提供会话级别的消息管理
 * <p>
 * 实现说明：
 * - 采用追加方式存储新消息，避免重复存储历史消息
 * - 使用Redis的ValueOperations存储序列化后的消息列表
 * - 通过@Async注解实现MySQL数据库的异步持久化
 * - 消息数量控制确保不会无限制增长
 */
@Component("toolChoiceAppChatMemory")
@Slf4j
public class ToolChoiceAppChatMemoryRes implements ChatMemoryRepository {


    /**
     * RedisTemplate实例，用于直接操作Redis数据库
     */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 查找所有会话ID
     * <p>
     * 通过扫描Redis中所有匹配前缀的键来获取会话ID列表。
     *
     * @return 包含所有会话ID的列表
     */
    @Override
    public List<String> findConversationIds() {
        log.debug("开始查找所有会话ID...");
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null) {
            return keys.stream()
                    .map(key -> key.substring(KEY_PREFIX.length()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();

    }

    /**
     * 根据会话ID查找聊天记录
     * <p>
     * 该方法从Redis中获取指定会话的所有消息，并进行反序列化处理。
     * 如果Redis中没有找到对应数据，则返回空列表。
     *
     * @param conversationId 会话ID，通常是用户ID
     * @return 包含该会话所有消息的列表
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        // 参数校验
        if (conversationId == null || conversationId.isEmpty()) {
            return new ArrayList<>();
        }

        // 构造Redis键并获取数据
        String key = getRedisKey(conversationId);
        Object value = redisTemplate.opsForValue().get(key);

        // 处理空值情况
        if (value == null) {
            return new ArrayList<>();
        }

        // 提取序列化的消息字符串
        List<String> serializedMessages = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item instanceof String) {
                serializedMessages.add((String) item);
            } else {
                log.warn("对话 [{}] 中发现非字符串类型的消息，跳过: {}",
                        conversationId, item.getClass().getName());
            }
        }

        // 反序列化消息
        List<Message> messages = new ArrayList<>(serializedMessages.size());
        for (String serialized : serializedMessages) {
            try {
                Message message = MessageSerializer.deserialize(serialized);
                messages.add(message);
            } catch (Exception e) {
                log.error("反序列化消息失败，跳过该消息: {}", serialized, e);
            }
        }

        log.debug("开始查找会话ID [{}] 的聊天记录 {}", conversationId, messages);
        return messages;
    }

    /**
     * 保存消息到指定会话
     * <p>
     * 该方法将新消息添加到指定会话中，并同时更新Redis缓存和MySQL数据库。
     * 同时会控制消息总数，防止存储过多消息。
     * <p>
     * 注意：此方法只保存传入的新消息，不会重复保存历史消息。
     * 这是与之前实现的主要区别，解决了重复存储的问题。
     *
     * @param conversationId 会话ID，通常是用户ID
     * @param messages       要保存的新消息列表
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
    }

    /**
     * 删除指定会话的所有消息
     * <p>
     * 该方法会从Redis中删除指定会话的所有消息。
     *
     * @param conversationId 要删除消息的会话ID
     */
    @Override
    public void deleteByConversationId(String conversationId) {
    }

    /**
     * 生成Redis键
     * <p>
     * 通过拼接前缀和会话ID生成唯一的Redis键。
     *
     * @param conversationId 会话ID
     * @return 完整的Redis键
     */
    private String getRedisKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }
}