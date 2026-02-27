package org.roxycode.gui;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class TemplateServiceTest {

    @Inject
    TemplateService templateService;

    @Test
    void testRenderCodePreview() {
        String content = "Hello <World>";
        String result = templateService.render("templates/code-preview.html", Map.of("content", content));
        assertTrue(result.contains("Hello &lt;World&gt;"), "Should escape HTML characters");
        assertTrue(result.contains("hljs.highlightAll()"), "Should contain highlight.js call");
    }
}
