package com.swj.shiwujie.service;

import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

public interface ChatService {

    /**
     * 与大模型文字交流（流式）
     *
     * @param text    输入的文本
     * @param blindId 用户盲ID
     * @return 大模型流式回复
     */
    Flux<String> doChatWithTextSSE(String text, Long blindId);


    Flux<String> imageHandle(MultipartFile imageFile);
}
