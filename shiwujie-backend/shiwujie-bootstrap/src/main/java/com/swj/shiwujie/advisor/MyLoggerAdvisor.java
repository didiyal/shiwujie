package com.swj.shiwujie.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {
    private int order;

    public MyLoggerAdvisor() {
        this.order = 10086;
    }


    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.debug("请求: {},token大小: {}", chatClientRequest.prompt().getUserMessage().getText(), chatClientRequest.prompt().getUserMessage().getText().length());
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        log.debug("响应: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
        return chatClientResponse;
    }

    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        log.debug("请求: {},token大小: {}", chatClientRequest.prompt().getUserMessage().getText(), chatClientRequest.prompt().getUserMessage().getText().length());
        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponses, (chatClientResponse) -> {
            log.debug("流式响应: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
        });
    }


    public String getName() {
        return this.getClass().getSimpleName();
    }

    public int getOrder() {
        return this.order;
    }

}