package org.roxycode.gui;

import com.google.genai.types.CachedContent;
import io.micronaut.context.annotation.Prototype;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.web.WebView;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import org.roxycode.jsmashy.core.ProjectFile;
import org.roxycode.jsmashy.core.RepositoryScanner;
import org.roxycode.jsmashy.formatters.XmlSmashFormatter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Prototype
public class CacheListController {

    @FXML
    private TableView<CacheRow> cacheTable;

    @FXML
    private TableColumn<CacheRow, String> nameColumn;

    @FXML
    private TableColumn<CacheRow, String> createTimeColumn;

    @FXML
    private TableColumn<CacheRow, String> expireTimeColumn;

    @FXML
    private TableColumn<CacheRow, Number> tokensColumn;

    @FXML
    private TableColumn<CacheRow, Void> actionsColumn;

    @Inject
    private GeminiService geminiService;

    @Inject
    private ProjectService projectService;

    @FXML
    private Label localProjectPathLabel;

    @FXML
    private Label localCachePathLabel;

    @FXML
    private Label localCacheTimeLabel;

    @FXML
    private Label localCacheSizeLabel;

    @FXML
    private Label localTokensLabel;

    @FXML
    private WebView contentWebView;

    @FXML
    private Label localStatusLabel;

    @FXML
    private Button cacheButton;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().displayName()));
        createTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().createTime()));
        expireTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().expireTime()));
        tokensColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().totalTokens()));
        actionsColumn.setCellFactory(col -> new TableCell<>() {

            private final Button deleteBtn = new Button("Delete");

            {
                deleteBtn.getStyleClass().add("delete-button");
                deleteBtn.setOnAction(e -> {
                    CacheRow row = getTableView().getItems().get(getIndex());
                    deleteCache(row.resourceName());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
        refresh();
        // Bind local cache info
        localProjectPathLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            String path = projectService.getProjectPath();
            return path != null ? path : "No project selected";
        }, projectService.projectPathProperty()));
        cacheButton.disableProperty().bind(projectService.projectPathProperty().isNull());
        localCachePathLabel.textProperty().bind(projectService.localCachePathProperty());
        localCacheSizeLabel.textProperty().bind(projectService.localCacheSizeProperty());
        localCacheTimeLabel.textProperty().bind(projectService.localCacheTimeProperty());
        localTokensLabel.textProperty().bind(projectService.localCacheTokensProperty());
        projectService.localCachePathProperty().addListener((obs, oldVal, newVal) -> {
            updateContentPreview();
        });
        updateContentPreview();
    }

    private void updateContentPreview() {
        String pathStr = projectService.getProjectPath();
        if (pathStr == null) {
            if (contentWebView != null) {
                Platform.runLater(() -> contentWebView.getEngine().loadContent("<html><body>No project selected</body></html>"));
            }
            return;
        }
        Path cachePath = Paths.get(pathStr, ProjectService.ROXY_DIR, ProjectService.CACHE_DIR, ProjectService.CACHE_FILE);
        File cacheFile = cachePath.toFile();
        if (cacheFile.exists()) {
            try {
                long bytes = cacheFile.length();
                String content;
                if (bytes > 1024 * 1024) {
                    byte[] buffer = new byte[1024 * 1024];
                    try (java.io.InputStream is = Files.newInputStream(cachePath)) {
                        is.read(buffer);
                    }
                    content = new String(buffer) + "\n\n... [Truncated due to 1MB limit]";
                } else {
                    content = Files.readString(cachePath);
                }
                loadHighlightedContent(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (contentWebView != null) {
                Platform.runLater(() -> contentWebView.getEngine().loadContent("<html><body>No cache found</body></html>"));
            }
        }
    }

    @FXML
    private void refresh() {
        new Thread(() -> {
            try {
                List<CachedContent> caches = geminiService.listCaches();
                List<CacheRow> rows = caches.stream().map(c -> new CacheRow(c.name().orElse(""), c.displayName().orElse(c.name().orElse("Unnamed")), c.createTime().map(Object::toString).orElse("-"), c.expireTime().map(Object::toString).orElse("-"), c.usageMetadata().flatMap(um -> um.totalTokenCount()).orElse(0))).collect(Collectors.toList());
                Platform.runLater(() -> {
                    cacheTable.setItems(FXCollections.observableArrayList(rows));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteCache(String name) {
        new Thread(() -> {
            try {
                geminiService.deleteCache(name);
                refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public record CacheRow(String resourceName, String displayName, String createTime, String expireTime, long totalTokens) {
    }

    private void loadHighlightedContent(String content) {
        if (contentWebView == null)
            return;
        String escaped = content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
        String html = "<html><head>" + "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css'>" + "<script src='https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js'></script>" + "<style>body { margin: 0; background: #f8f9fa; } pre { margin: 0; padding: 15px; font-family: monospace; font-size: 12px; }</style>" + "</head><body>" + "<pre><code class='language-xml'>" + escaped + "</code></pre>" + "<script>hljs.highlightAll();</script>" + "</body></html>";
        Platform.runLater(() -> contentWebView.getEngine().loadContent(html));
    }

    @FXML
    private void handleCacheCodebase() {
        String pathStr = projectService.getProjectPath();
        if (pathStr == null)
            return;
        File selectedDirectory = new File(pathStr);
        localStatusLabel.setText("Status: Scanning...");
        CompletableFuture.runAsync(() -> {
            try {
                RepositoryScanner scanner = new RepositoryScanner();
                List<ProjectFile> files = scanner.scan(selectedDirectory.toPath());
                Platform.runLater(() -> localStatusLabel.setText("Status: Formatting..."));
                XmlSmashFormatter formatter = new XmlSmashFormatter();
                String xml = formatter.format(files);
                // Save to roxy/cache/codebase_cache.xml
                try {
                    Path cacheDir = selectedDirectory.toPath().resolve(ProjectService.ROXY_DIR).resolve(ProjectService.CACHE_DIR);
                    if (!Files.exists(cacheDir)) {
                        Files.createDirectories(cacheDir);
                    }
                    Files.writeString(cacheDir.resolve(ProjectService.CACHE_FILE), xml);
                } catch (Exception e) {
                    System.err.println("Failed to save local cache: " + e.getMessage());
                }
                // Refresh ProjectService metadata
                projectService.refreshLocalCacheInfo();
                // Token estimation check
                int minTokens = geminiService.getPrefs().getInt(SettingsController.CACHE_MIN_TOKENS, SettingsController.DEFAULT_MIN_TOKENS);
                int estimatedTokens = (int) (xml.length() / SettingsController.BYTES_PER_TOKEN);
                if (estimatedTokens < minTokens) {
                    Platform.runLater(() -> {
                        localStatusLabel.setText("Status: Too Small");
                    });
                    return;
                }
                Platform.runLater(() -> localStatusLabel.setText("Status: Caching..."));
                String model = geminiService.getPrefs().get(SettingsController.GEMINI_MODEL, SettingsController.DEFAULT_MODEL);
                CachedContent cache = geminiService.createCodebaseCache(xml, selectedDirectory.getName());
                String cacheName = cache.name().get();
                projectService.setCurrentCacheModel(model);
                projectService.setCurrentCacheName(cacheName);
                // Refresh both
                refresh();
                Platform.runLater(() -> {
                    localStatusLabel.setText("Status: Cached");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    localStatusLabel.setText("Status: Error");
                    e.printStackTrace();
                });
            }
        });
    }
}
