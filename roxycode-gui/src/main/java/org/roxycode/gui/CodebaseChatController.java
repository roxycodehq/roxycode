package org.roxycode.gui;

import com.google.genai.Chat;
import com.google.genai.types.CachedContent;
import com.google.genai.types.GenerateContentResponse;
import io.micronaut.context.annotation.Prototype;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import org.roxycode.gui.MarkdownRenderer;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.roxycode.jsmashy.core.ProjectFile;
import org.roxycode.jsmashy.core.RepositoryScanner;
import org.roxycode.jsmashy.formatters.XmlSmashFormatter;
import jakarta.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the Codebase Chat screen.
 */
@Prototype
public class CodebaseChatController {

    @FXML
    private Label pathLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private WebView chatWebView;

    @FXML
    private Button cacheButton;

    @FXML
    private TextField chatInput;

    @Inject
    private GeminiService geminiService;

    private File selectedDirectory;

    private Chat chat;

    private MarkdownRenderer markdownRenderer = new MarkdownRenderer();

    private WebEngine webEngine;

    @FXML
    public void initialize() {
        webEngine = chatWebView.getEngine();
        webEngine.loadContent("<html><head><link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css'><script src='https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js'></script><style>body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 14px; line-height: 1.5; color: #2f3542; background-color: transparent; margin: 0; padding: 15px; } .message-container { margin-bottom: 20px; display: flex; flex-direction: column; } .user-message-container { align-items: flex-end; } .ai-message-container { align-items: flex-start; } .bubble { padding: 10px 15px; border-radius: 15px; max-width: 85%; word-wrap: break-word; } .user-bubble { background-color: #3498db; color: white; } .ai-bubble { background-color: #f1f2f6; color: #2f3542; border: 1px solid #dfe4ea; } .system-bubble { font-style: italic; color: #747d8c; align-self: center; } pre { background-color: #f8f9fa; padding: 10px; border-radius: 8px; overflow-x: auto; border: 1px solid #dfe4ea; } code { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 0.9em; }</style></head><body><div id='chat-history'></div></body></html>");
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                addMessage("System", "Select a folder and cache it to start chatting.");
            }
        });
    }

    private void addMessage(String sender, String text) {
        // Correctly escape for JS string literal and HTML injection
        String htmlContent = markdownRenderer.render(text).replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        String containerClass = "message-container ";
        String bubbleClass = "bubble ";
        if ("User".equals(sender)) {
            containerClass += "user-message-container";
            bubbleClass += "user-bubble";
        } else if ("Gemini".equals(sender)) {
            containerClass += "ai-message-container";
            bubbleClass += "ai-bubble";
        } else {
            bubbleClass += "system-bubble";
        }
        String script = "(function() {" + "var historyElement = document.getElementById('chat-history');" + "var container = document.createElement('div');" + "container.className = '" + containerClass + "';" + "container.innerHTML = '<div class=\"" + bubbleClass + "\">' + '" + htmlContent + "' + '</div>';" + "if (historyElement) { historyElement.appendChild(container); }" + "if (typeof hljs !== 'undefined') { hljs.highlightAll(); }" + "window.scrollTo(0, document.body.scrollHeight);" + "})();";
        Platform.runLater(() -> {
            try {
                webEngine.executeScript(script);
            } catch (Exception e) {
                System.err.println("Error executing script: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Handles the folder selection action.
     */
    @FXML
    private void handleSelectFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Codebase Folder");
        File dir = directoryChooser.showDialog(new Stage());
        if (dir != null) {
            selectedDirectory = dir;
            pathLabel.setText(dir.getAbsolutePath());
            cacheButton.setDisable(false);
        }
    }

    /**
     * Handles the cache codebase action.
     */
    @FXML
    private void handleCacheCodebase() {
        if (selectedDirectory == null)
            return;
        statusLabel.setText("Status: Scanning...");
        cacheButton.setDisable(true);
        CompletableFuture.runAsync(() -> {
            try {
                RepositoryScanner scanner = new RepositoryScanner();
                List<ProjectFile> files = scanner.scan(selectedDirectory.toPath());
                Platform.runLater(() -> statusLabel.setText("Status: Formatting..."));
                XmlSmashFormatter formatter = new XmlSmashFormatter();
                String xml = formatter.format(files);
                Platform.runLater(() -> statusLabel.setText("Status: Caching..."));
                CachedContent cache = geminiService.createCodebaseCache(xml, selectedDirectory.getName());
                chat = geminiService.startChat(cache.name().get());
                Platform.runLater(() -> {
                    statusLabel.setText("Status: Cached");
                    addMessage("System", "Codebase cached. You can now ask questions.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Status: Error");
                    addMessage("Error", e.getMessage());
                    cacheButton.setDisable(false);
                });
            }
        });
    }

    /**
     * Handles the send message action.
     */
    @FXML
    private void handleSendMessage() {
        String input = chatInput.getText();
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        if (chat == null) {
            addMessage("System", "Please select a folder and click 'Cache Codebase' before chatting.");
            return;
        }
        chatInput.clear();
        addMessage("User", input);
        statusLabel.setText("Status: Thinking...");
        CompletableFuture.runAsync(() -> {
            try {
                GenerateContentResponse response = chat.sendMessage(input);
                String text = response.text();
                Platform.runLater(() -> {
                    addMessage("Gemini", text);
                    statusLabel.setText("Status: Ready");
                });
            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Platform.runLater(() -> {
                    addMessage("Error", "Gemini Error: " + errorMessage);
                    statusLabel.setText("Status: Error");
                });
            }
        });
    }
}
