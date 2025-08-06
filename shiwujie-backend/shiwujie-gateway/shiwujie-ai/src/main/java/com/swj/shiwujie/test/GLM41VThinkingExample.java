package com.swj.shiwujie.test;


import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.*;

import java.util.Arrays;

public class GLM41VThinkingExample {
public static void main(String[] args) {
    String apiKey = "2d864171869b45a09522796814af4759.ESOf9pM3msOz1Uuk"; // 请填写您自己的APIKey
    ZhipuAiClient client = ZhipuAiClient.builder()
        .apiKey(apiKey)
        .build();

    ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
        .model("glm-4.1v-thinking-flashx")
        .messages(Arrays.asList(
            ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(Arrays.asList(
                    MessageContent.builder()
                        .type("text")
                        .text("描述下这张图片")
                        .build(),
                    MessageContent.builder()
                        .type("image_url")
                        .imageUrl(ImageUrl.builder()
                        .url("https://aigc-files.bigmodel.cn/api/cogview/20250723213827da171a419b9b4906_0.png")
                        .build())
                    .build()))
                .build()
        ))
        .build();

    ChatCompletionResponse response = client.chat().createChatCompletion(request);

    if (response.isSuccess()) {
        Object reply = response.getData().getChoices().get(0).getMessage().getContent();
        System.out.println(reply);
    } else {
        System.err.println("错误: " + response.getMsg());
    }
}
}