package com.swj.shiwujie.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;


/**
 * 线程安全的Message序列化工具类
 */
@Component
public class MessageSerializer {

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

            return Base64.getEncoder().encodeToString(baos.toByteArray());
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
        } catch (IOException e) {
            throw new RuntimeException("消息反序列化失败", e);
        }
    }
}
