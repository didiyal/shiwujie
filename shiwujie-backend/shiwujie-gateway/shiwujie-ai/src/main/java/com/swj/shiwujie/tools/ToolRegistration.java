package com.swj.shiwujie.tools;

import com.swj.shiwujie.tools.app.CommunityTools;
import com.swj.shiwujie.tools.app.UserTools;
import com.swj.shiwujie.tools.app.WorkChooseTool;
import com.swj.shiwujie.tools.mytools.WebScrapingTool;
import com.swj.shiwujie.tools.mytools.WebSearchTool;
import jakarta.annotation.Resource;
import org.apache.catalina.User;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 工具统一注册类
 */
@Configuration
public class ToolRegistration {



    @Resource
    private WorkChooseTool workChooseTool;


    @Resource
    private WebSearchTool webSearchTool;

    @Bean
    public ToolCallback[] allTools() {
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        return ToolCallbacks.from(
                webSearchTool,
                webScrapingTool,
                workChooseTool
        );
    }


}
