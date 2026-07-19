package com.swj.shiwujie.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring {@link ApplicationContext} 静态持有器。
 *
 * <p>用途：{@code @ServerEndpoint}（jakarta WS）实例由 WS 容器按 session 反射 new，<b>不经 Spring DI</b>——
 * 在 WS 处理器里需要 Spring bean 时（如 AI 中继服务、{@code InnerBlindService}），用 {@link #getBean(Class)}
 * 兜底取用。本 bean 由 {@code @Component} + 全包扫描注册，启动期 {@link #setApplicationContext} 注入静态域，
 * 运行期（首个 WS 连接、消息到达）context 必已就绪。</p>
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {


    private static ApplicationContext context;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }


    /**
     * 按类型取 bean；context 未注入（应用未启动完成）抛 {@link IllegalStateException}——fail-fast，
     * 不静默返回 null 把问题推到运行期。
     */
    public static <T> T getBean(Class<T> requiredType) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext 尚未注入（应用未启动完成？）");
        }
        return context.getBean(requiredType);
    }


}
