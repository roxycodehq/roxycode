package org.roxycode.gui;

import io.micronaut.context.annotation.Prototype;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import jakarta.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import io.micronaut.context.ApplicationContext;

@Prototype
public class CacheDetailsController {

    @FXML
    private Label pathLabel;
    @FXML
    private Label sizeLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private WebView contentWebView;

    @Inject
    private ProjectService projectService;
    @Inject
    private ApplicationContext context;

    @FXML
    public void initialize() {
        pathLabel.textProperty().bind(projectService.localCachePathProperty());
        sizeLabel.textProperty().bind(projectService.localCacheSizeProperty());
        timeLabel.textProperty().bind(projectService.localCacheTimeProperty());
        
        projectService.localCachePathProperty().addListener((obs, oldVal, newVal) -> updateContentPreview());
        updateContentPreview();
    }

    private void updateContentPreview() {
        String projectPath = projectService.getProjectPath();
        if (projectPath == null) {
            contentWebView.getEngine().loadContent("<html><body>No project selected</body></html>");
            return;
        }

        Path cachePath = Paths.get(projectPath, ".roxy", "codebase_cache.xml");
        File file = cachePath.toFile();

        if (file.exists()) {
            try {
                // Load content (limit to 1MB)
                long size = file.length();
                String content;
                if (size > 1024 * 1024) {
                    byte[] bytes = new byte[1024 * 1024];
                    try (java.io.InputStream is = Files.newInputStream(cachePath)) {
                        is.read(bytes);
                    }
                    content = new String(bytes) + "\n\n... [Truncated due to 1MB limit]";
                } else {
                    content = Files.readString(cachePath);
                }
                
                loadHighlightedContent(content);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            contentWebView.getEngine().loadContent("<html><body>No cache found</body></html>");
        }
    }

    private void loadHighlightedContent(String content) {
        String escaped = content.replace("&", "&amp;")
                               .replace("<", "&lt;")
                               .replace(">", "&gt;")
                               .replace("\"", "&quot;")
                               .replace("'", "&#39;");

        String html = "<html><head>" +
                "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css'>" +
                "<script src='https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js'></script>" +
                "<style>body { margin: 0; background: #f8f9fa; } pre { margin: 0; padding: 15px; font-family: monospace; font-size: 12px; }</style>" +
                "</head><body>" +
                "<pre><code class='language-xml'>" + escaped + "</code></pre>" +
                "<script>hljs.highlightAll();</script>" +
                "</body></html>";
        contentWebView.getEngine().loadContent(html);
    }

    @FXML
    private void handleBack() {
        context.getBean(MainController.class).showChat();
    }
}