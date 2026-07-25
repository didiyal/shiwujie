package com.swj.shiwujie.ai;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import static com.swj.shiwujie.constants.AiConstants.IMAGE_MODEL;
import static com.swj.shiwujie.constants.AiConstants.TEXT_MODEL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 冒烟测试：验证 DashScope key + 文本/图像 model 能真正返回（端到端打通）。
 *
 * <p>性质：<b>集成测试</b>——真调 DashScope（需联网 + 有效 key + 产生 token 费用），与现有
 * 286 个纯 Mockito 单元测试<b>不同</b>。为避免污染常规 {@code mvn test}，默认由
 * {@link EnabledIfSystemProperty} 守卫跳过；需验证时加开关单独跑：
 * <pre>{@code
 *   mvn -f shiwujie-backend/pom.xml test -Dtest=AiSmokeTest -Dai.smoke=true -Dsurefire.failIfNoSpecifiedTests=false
 * }</pre>
 *
 * <p>三个用例（与 AiConfig 两条路径对应）：
 * <ul>
 *   <li>{@link #textModel_returnsContent()} —— 文本模型，<b>镜像 AiConfig.qwenText：OpenAI 兼容直连</b>
 *       （DashScope compatible-mode）。spring-ai-alibaba 的 DashScopeChatModel 调 qwen3.x 文本报
 *       {@code url error}，故文本路径改用 OpenAiChatModel。</li>
 *   <li>{@link #rawHttp_compatibleMode()} —— 裸 HTTP 直调，绕开所有客户端（隔离变量、定位根因用）。</li>
 *   <li>{@link #imageModel_recognizes()} —— 图像模型，镜像 AiConfig.qwenImage（DashScope 多模态，正常）。</li>
 * </ul>
 *
 * <p>model 名引用 {@link com.swj.shiwujie.constants.AiConstants}（= AiConfig 实际装配值），改常量测试
 * 自动跟随；API key / base-url 取环境变量，未设则用 application.yml 默认值。
 *
 * <p>注：AI 模块后续会用别的技术重构（见 ROADMAP），本测试为临时冒烟验证，不追求精致。
 */
@EnabledIfSystemProperty(named = "ai.smoke", matches = "true")
class AiSmokeTest {

    // 与 application.yml 默认值一致（项目约定：凭据留 yml 用 ${ENV:默认} 占位符）
    private static final String API_KEY =
            System.getenv().getOrDefault("DASHSCOPE_API_KEY", "sk-6374b4a1ebd64f56ae8d0799e74b7927");
    private static final String BASE_URL =
            System.getenv().getOrDefault("DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode");

    private static DashScopeApi dashScopeApi() {
        return DashScopeApi.builder().apiKey(API_KEY).build();
    }

    @Test
    @DisplayName("文本模型 " + TEXT_MODEL + "（= AiConfig.qwenText / OpenAI 兼容直连）：能正常返回非空文本")
    void textModel_returnsContent() {
        // 与 AiConfig.qwenText 构造一致：OpenAiChatModel + compatible-mode base-url
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(BASE_URL)
                .apiKey(API_KEY)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(TEXT_MODEL).build())
                .build();
        String reply = ChatClient.builder(model)
                .build()
                .prompt()
                .user("用一句中文回复：测试通过")
                .call()
                .content();
        System.out.println("[文本 " + TEXT_MODEL + "] -> " + reply);
        assertThat(reply).as("文本模型应返回非空内容").isNotBlank();
    }

    @Test
    @DisplayName("裸 HTTP 对照：绕开任何客户端，直调 compatible-mode（隔离变量）")
    void rawHttp_compatibleMode() throws Exception {
        // 用 JDK HttpClient 直接 POST DashScope OpenAI 兼容端点，不经任何 ChatModel 客户端。
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        String body = "{\"model\":\"" + TEXT_MODEL
                + "\",\"messages\":[{\"role\":\"user\",\"content\":\"用一句中文回复：测试通过\"}]}";
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(BASE_URL + "/v1/chat/completions"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        java.net.http.HttpResponse<String> resp =
                client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        System.out.println("[裸HTTP " + TEXT_MODEL + "] status=" + resp.statusCode() + " body=" + resp.body());
        assertThat(resp.statusCode())
                .as("裸 HTTP 直调应 200；实返回 body=" + resp.body())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("图像模型 " + IMAGE_MODEL + "（= AiConfig.qwenImage / DashScope）：能识别图片并返回非空描述")
    void imageModel_recognizes() throws Exception {
        // 与 AiConfig.qwenImage 构造一致（withMultiModel(true)）
        DashScopeChatModel model = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi())
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(IMAGE_MODEL)
                        .withMultiModel(true)
                        .build())
                .build();
        File png = writeTestPng();
        String reply = ChatClient.builder(model)
                .build()
                .prompt()
                .user(u -> u.text("用一句话描述这张图片的内容")
                        .media(MimeTypeUtils.IMAGE_PNG, new FileSystemResource(png)))
                .call()
                .content();
        System.out.println("[图像 " + IMAGE_MODEL + "] -> " + reply);
        assertThat(reply).as("图像模型应返回非空描述").isNotBlank();
    }

    /** 生成一张红底白字 "TEST" 的小 PNG（避免依赖外部图片素材）。 */
    private static File writeTestPng() throws Exception {
        BufferedImage bi = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 200, 100);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        g.drawString("TEST", 50, 65);
        g.dispose();
        File f = File.createTempFile("ai-smoke-", ".png");
        f.deleteOnExit();
        ImageIO.write(bi, "png", f);
        return f;
    }
}
