package com.swj.shiwujie.tools.mytools;

import com.swj.shiwujie.app.ImageApp;
import com.swj.shiwujie.constants.AiConstants;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.utils.LoginUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AiModelTools {

    @Resource
    private ImageApp imageApp;


    /**
     * 图片信息追问
     */
    @Tool(name = "Ask about image information",
            description = "Call the image model to analyze the content of the inquiry. Check if there is image information (image: ***) in the context before calling.")
    public String TakePhoto(@ToolParam (description = "Content of user's inquiry") String  message) {
        log.info("图片信息追问{}", message);
        return imageApp.doChatCall(message, LoginUtils.getLoginBlind().getBlindId());
    }



    /**
     * 切换回答语气
     */
    @Tool(name = "Switch tone")
    public String switchModelTone(@ToolParam (description = "Tone to switch to") String tone) {
        log.info("切换语气{}", tone);
        Blind blind = LoginUtils.getLoginBlind();
        Long blindId = blind.getBlindId();
        AiConstants.clientTone.put(blindId, tone);
        return "已切换至" + tone + "语气";
    }

}
