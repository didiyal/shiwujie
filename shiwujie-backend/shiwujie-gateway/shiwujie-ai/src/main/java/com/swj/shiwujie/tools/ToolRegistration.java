package com.swj.shiwujie.tools;

import com.swj.shiwujie.tools.app.CommunityTools;
import com.swj.shiwujie.tools.app.UserTools;
import com.swj.shiwujie.tools.app.WorkChooseTool;
import com.swj.shiwujie.tools.mytools.WebScrapingTool;
import com.swj.shiwujie.tools.mytools.WebSearchTool;
import jakarta.annotation.Resource;
import org.apache.catalina.User;
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

    @Resource
    private CommunityTools communityTools;


    @Bean
    public ToolCallback[] allTools() {
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        WorkChooseTool workChooseTool = new WorkChooseTool(userTools, communityTools);
        return ToolCallbacks.from(
                webSearchTool,
                webScrapingTool,
                workChooseTool
        );
    }


}
