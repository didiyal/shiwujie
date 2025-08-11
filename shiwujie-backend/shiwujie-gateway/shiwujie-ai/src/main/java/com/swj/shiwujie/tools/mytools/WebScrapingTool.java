package com.swj.shiwujie.tools.mytools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;

/**
 * 网页抓取
 */
public class WebScrapingTool {

    @Tool(name = "URL抓取", description = "用于获取指定URL的完整网页HTML内容。当需要分析某个具体网页的详细内容（如文章全文、页面结构、特定元素）时使用，输入参数为有效的网页URL地址")
    public String scrapeWebPage(@ToolParam(description = "需要抓取内容的网页URL，必须是完整有效的HTTP/HTTPS地址（如https://example.com/article）") String url) {
        // 实现代码保持不变
        try {
            Document doc = Jsoup.connect(url).get();
            return doc.html();
        } catch (IOException e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
