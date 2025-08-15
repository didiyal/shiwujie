package com.swj.shiwujie.tools.mytools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSearchToolTest {

    @Test
    void searchWeb() {
        WebSearchTool webSearchTool = new WebSearchTool();
        String result = webSearchTool.searchWeb("2024年人工智能发展趋势");
        System.out.println(result);
    }
}