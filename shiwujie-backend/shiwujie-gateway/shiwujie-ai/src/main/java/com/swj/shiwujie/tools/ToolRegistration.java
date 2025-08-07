package com.swj.shiwujie.tools;

import com.swj.shiwujie.tools.app.UserTools;
import com.swj.shiwujie.tools.mytools.WebScrapingTool;
import com.swj.shiwujie.tools.mytools.WebSearchTool;
import jakarta.annotation.Resource;
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

    @Value("${search-api.api-key}")
    private String searchApiKey;



    @Resource
    private UserTools userTools;

    @Bean
    public ToolCallback[] allTools() {
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        return ToolCallbacks.from(
                webSearchTool,
                webScrapingTool,
                userTools
        );
    }


}
