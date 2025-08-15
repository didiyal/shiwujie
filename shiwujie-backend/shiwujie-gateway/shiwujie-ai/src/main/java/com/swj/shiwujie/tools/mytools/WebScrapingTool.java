package com.swj.shiwujie.tools.mytools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 网页分析
 */
public class WebScrapingTool {

    /**
     * 网页分析工具
     * @param url
     * @return
     */
    public String scrapeWebPage(String url) {
        // 实现代码保持不变
        try {
            Document doc = Jsoup.connect(url).get();
            return doc.html();
        } catch (IOException e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }


    /**
     * 批量网页分析工具
     * @param urls
     * @return
     */
    public String scrapeWebPage(List<String> urls) {
        // 实现代码保持不变
        List<String> docs = new ArrayList<>();
        try {
            for (String url : urls) {
                Document doc = Jsoup.connect(url).get();
                docs.add(doc.html());
            }
        } catch (Exception e) {
            return "批量读取网页失败: " + e.getMessage();
        }
        return String.join("\n\n--------------------\n\n", docs);
    }
}
