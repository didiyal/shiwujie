package com.swj.shiwujie.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 轻量级消息序列化工具类
 * 避免将大图片数据序列化到数据库中
 */
@Component
public class LightweightMessageSerializer {

    // 使用ThreadLocal确保Kryo实例的线程安全
    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        // 设置实例化策略，兼容无参构造函数的类
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        return kryo;
    });

    /**
     * 使用Kryo将Message序列化为Base64字符串
     * 对于大消息进行截断处理，避免数据库字段溢出
     */
    public static String serialize(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("消息对象不能为null");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Output output = new Output(baos)) {

            // 获取当前线程的Kryo实例
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            kryo.writeClassAndObject(output, message);
            output.flush();

            byte[] data = baos.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(data);
            
            // MySQL的LONGTEXT最大可以存储4GB数据，一般来说足够使用
            // 如果需要限制大小，可以取消下面的注释

            if (base64.length() > 65535) {
                // 如果序列化后的内容超过65KB，进行截断处理并记录日志
                System.err.println("警告：消息内容过长，已截断: " + base64.length() + " 字符");
                base64 = base64.substring(0, 65535);
            }

            
            return base64;
        } catch (IOException e) {
            throw new RuntimeException("消息序列化失败", e);
        }
    }

    /**
     * 使用Kryo将Base64字符串反序列化为Message对象
     */
    public static Message deserialize(String base64) {
        if (base64 == null || base64.isEmpty()) {
            throw new IllegalArgumentException("序列化字符串不能为null或空");
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             Input input = new Input(bais)) {

            // 获取当前线程的Kryo实例
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            return (Message) kryo.readClassAndObject(input);
        } catch (Exception e) {
            // 如果反序列化失败，创建一个默认的消息对象
            System.err.println("消息反序列化失败，创建默认消息对象: " + e.getMessage());
            return new AssistantMessage("【消息内容已损坏或过长】");
        }
    }
}
