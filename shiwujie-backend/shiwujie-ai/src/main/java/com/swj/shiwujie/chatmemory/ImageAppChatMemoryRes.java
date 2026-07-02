package com.swj.shiwujie.chatmemory;

import com.swj.shiwujie.model.domain.ai.AiLogs;
import com.swj.shiwujie.service.AiLogsService;
import com.swj.shiwujie.utils.MessageSerializer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.swj.shiwujie.constants.AiConstants.KEY_PREFIX;


/**
 * imageApp聊天内存仓库
 */
@Component("imageAppChatMemory")
@Slf4j
public class ImageAppChatMemoryRes implements ChatMemoryRepository {

    /**
     * RedisTemplate实例，用于直接操作Redis数据库
     */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * AI日志服务实例，用于将聊天记录持久化到MySQL数据库
     */
    @Resource
    private AiLogsService aiLogsService;


    /**
     * 查找所有会话ID
     * <p>
     * 通过扫描Redis中所有匹配前缀的键来获取会话ID列表。
     *
     * @return 包含所有会话ID的列表
     */
    @Override
    public List<String> findConversationIds() {
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
                MessageType messageType = message.getMessageType();
                if (messageType == MessageType.USER) {
                    UserMessage userMessage = (UserMessage) message;
                    String text = userMessage.getText();
                    if (text.contains("image")) {
                        // 从数据库中查找图片
                        String[] split = text.split("image");
                        String logIdStr = split[1];
                        long logId = Long.parseLong(logIdStr);
                        AiLogs logEntry = aiLogsService.getById(logId);
                        // 创建图像信息
                        UserMessage deserialize = (UserMessage) MessageSerializer.deserialize(logEntry.getContent());
                        message =  UserMessage.builder()
                                .media(deserialize.getMedia())// 设置图像信息
                                .text("image" + logIdStr)
                                .build();
                    }
                }
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
        // 参数校验
        if (conversationId == null || conversationId.isEmpty() || messages == null) {
            return;
        }

        // 将最后一条消息存入数据库中，将其余的图片消息消除
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            MessageType messageType = message.getMessageType();
            // 只有用户消息才可能有图片信息
            if (messageType == MessageType.USER) {
                UserMessage userMessage = (UserMessage) message;
                // 遍历Media元信息
                for (Media media : userMessage.getMedia()) {
                    byte[] imageByte = media.getDataAsByteArray();
                    // 如果是最后一条消息，则保存图片信息，文字信息也保存
                    if(i == messages.size() - 1 ){
                        userMessage = savelastUserMessage(conversationId, messages, imageByte, message, userMessage);
                    }else {
                        // 如果不是最后一条消息，则判断图片大小，有图片则脱敏
                        if(imageByte.length > 1000){
                            userMessage = new UserMessage(userMessage.getText());
                            messages.remove(message);
                            messages.add(userMessage);
                        }
                    }
                }


            }

        }


        // 直接保存传入的消息列表到Redis
        setToRedis(conversationId, messages);

        log.debug("开始保存会话[{}]消息，保存成功，对话中有 {} 条消息 {}",
                conversationId, messages.size(), messages);
    }

    private UserMessage savelastUserMessage(String conversationId, List<Message> messages, byte[] imageByte, Message message, UserMessage userMessage) {
        if (imageByte.length > 1000) {
            // 图像信息存入数据库并创建图像信息
            Long userId = Long.parseLong(conversationId);
            AiLogs logEntry = new AiLogs();
            logEntry.setOperatorId(userId);
            logEntry.setContent(MessageSerializer.serialize(message));
            aiLogsService.save(logEntry);

            // 新建图像信息到上下文中
            Long logId = logEntry.getLogId();
            String content = "帮我识别这张图片:image" + logId;
            userMessage = new UserMessage(content);
            messages.remove(message);
            messages.add(userMessage);
        }else {
            Long userId = Long.parseLong(conversationId);
            AiLogs logEntry = new AiLogs();
            logEntry.setOperatorId(userId);
            logEntry.setContent(MessageSerializer.serialize(message));
            aiLogsService.save(logEntry);
        }
        return userMessage;
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
        log.debug("开始删除会话ID [{}] 的所有消息...", conversationId);
        // 参数校验
        if (conversationId == null || conversationId.isEmpty()) {
            return;
        }
        // 构造Redis键并删除数据
        String key = getRedisKey(conversationId);
        redisTemplate.delete(key);
        log.debug("已清空对话 [{}] 的历史消息", conversationId);

    }

    /**
     * 将消息列表存储到Redis
     * <p>
     * 该方法会将消息列表序列化后存储到Redis中，并设置5天的过期时间。
     *
     * @param conversationId 会话ID
     * @param messages       要存储的消息列表
     */
    private void setToRedis(String conversationId, List<Message> messages) {

        String key = getRedisKey(conversationId);
        List<String> serializedMessages = new ArrayList<>(messages.size());
        // 序列化每条消息
        for (Message message : messages) {
            try {
                String serialized = MessageSerializer.serialize(message);
                serializedMessages.add(serialized);
            } catch (Exception e) {
                log.error("序列化消息失败，跳过该消息: {}", message, e);
            }
        }
        // 存储到Redis并设置5天过期时间
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, serializedMessages, 5, TimeUnit.DAYS);

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
