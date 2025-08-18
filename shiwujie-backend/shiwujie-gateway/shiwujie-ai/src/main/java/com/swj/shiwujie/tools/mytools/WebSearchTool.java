package com.swj.shiwujie.tools.mytools;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 网页搜索工具
 */
@Component
public class WebSearchTool {

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Value("${search-api.api-key}")
    private String apiKey;

    /**
     * 搜索网页内容并自动获取详细信息
     * @param query 搜索关键词
     * @return 搜索结果及详细内容的简化版本
     */
    public String searchWeb(String query) {
        // 添加参数验证，防止null或空字符串导致的错误
        if (query == null || query.trim().isEmpty()) {
            return "错误：搜索关键词不能为空";
        }
        
        try {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("q", query);
            paramMap.put("api_key", apiKey);
            paramMap.put("engine", "baidu");
            
            String response = cn.hutool.http.HttpUtil.get(SEARCH_API_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            
            if (organicResults == null || organicResults.isEmpty()) {
                return "未找到相关搜索结果";
            }
            
            // 取前3个结果以避免内容过多
            int resultCount = Math.min(3, organicResults.size());
            
            StringBuilder result = new StringBuilder();
            result.append("关于 \"").append(query).append("\" 的搜索结果:\n\n");
            
            for (int i = 0; i < resultCount; i++) {
                JSONObject item = (JSONObject) organicResults.get(i);
                String title = item.getStr("title", "无标题");
                String link = item.getStr("link", "");
                String snippet = item.getStr("snippet", "无描述");
                
                result.append("结果 ").append(i + 1).append(":\n");
                result.append("标题: ").append(title).append("\n");
                
                // 尝试获取网页详细内容
                String content = getWebPageSummary(link);
                if (!content.startsWith("获取失败")) {
                    result.append("内容: ").append(content).append("\n");
                } else {
                    // 如果无法获取详细内容，则使用摘要
                    result.append("摘要: ").append(snippet).append("\n");
                }
                result.append("\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            return "搜索失败: " + e.getMessage();
        }
    }

    /**
     * 获取网页内容摘要
     * @param url 网页链接
     * @return 简化后的网页内容
     */
    private String getWebPageSummary(String url) {
        try {
            Document document = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .timeout(10000)
                    .followRedirects(true)
                    .get();
            
            // 移除script和style元素
            document.select("script").remove();
            document.select("style").remove();
            
            StringBuilder content = new StringBuilder();
            
            // 获取页面标题
            Element titleElement = document.select("title").first();
            if (titleElement != null) {
                content.append("【页面标题】").append(titleElement.text()).append("\n");
            }
            
            // 获取正文内容
            Element body = document.body();
            if (body != null) {
                // 移除常见的无关元素
                body.select("nav, header, footer, aside, .sidebar, .advertisement, .ads, .comment, .breadcrumb, .menu, .navigation").remove();
                
                String text = body.text().trim();
                if (!text.isEmpty()) {
                    // 清理多余的空白字符
                    text = text.replaceAll("\\s+", " ");
                    
                    // 提取前800个字符作为摘要
                    if (text.length() > 800) {
                        text = text.substring(0, 800) + "...";
                    }
                    content.append("【页面内容】").append(text);
                }
            }
            
            return content.toString();
        } catch (IOException e) {
            return "获取失败(网络错误): " + e.getMessage();
        } catch (Exception e) {
            return "获取失败(解析错误): " + e.getMessage();
        }
    }
}