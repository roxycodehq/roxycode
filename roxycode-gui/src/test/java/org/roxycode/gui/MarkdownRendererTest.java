package org.roxycode.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarkdownRendererTest {

    @Test
    void testBasicMarkdown() {
        MarkdownRenderer renderer = new MarkdownRenderer();
        String result = renderer.render("# Hello\n**Bold**");
        assertTrue(result.contains("<h1>Hello</h1>"));
        assertTrue(result.contains("<strong>Bold</strong>"));
    }

    @Test
    void testCodeBlocks() {
        MarkdownRenderer renderer = new MarkdownRenderer();
        String result = renderer.render("```java\nSystem.out.println(\"test\");\n```");
        assertTrue(result.contains("<pre><code class=\"language-java\">"));
    }
}
