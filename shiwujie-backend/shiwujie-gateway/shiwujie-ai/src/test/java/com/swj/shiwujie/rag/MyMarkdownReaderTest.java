package com.swj.shiwujie.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class MyMarkdownReaderTest {


    @Resource
    private MyMarkdownReader myMarkdownReader;

    @Test
    void loadMarkdown() {

        myMarkdownReader.loadMarkdown();
    }
}