package org.roxycode.gui;

import com.google.genai.Chat;
import com.google.genai.types.CachedContent;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.Part;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
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
import jakarta.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.roxycode.jsmashy.core.RepositoryScanner;
import org.roxycode.jsmashy.core.ProjectFile;
import org.roxycode.jsmashy.formatters.XmlSmashFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Codebase Chat screen.
 */
@Prototype
public class CodebaseChatController {
    private static final Logger LOG = LoggerFactory.getLogger(CodebaseChatController.class);

    @FXML
    private Label pathLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label sizeLabel;

    @FXML
    private Label tokenLabel;

    @FXML
    private WebView chatWebView;


    @FXML
    private TextField chatInput;

    @FXML
    private ComboBox<Agent> agentComboBox;

    @Inject
    private AgentService agentService;

    @Inject
    private GeminiService geminiService;

    @Inject
    private ProjectService projectService;

    @Inject
    private io.micronaut.context.ApplicationContext context;

    @Inject
    private AgentScriptService agentScriptService;

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
                restoreSession();
                Platform.runLater(() -> {
                    agentComboBox.getItems().addAll(agentService.getAgents());
                    if (!agentComboBox.getItems().isEmpty()) {
                        agentComboBox.getSelectionModel().select(0);
                    }
                });
            }
        });
        // Restore project path if available
        String savedPath = projectService.getProjectPath();
        if (savedPath != null) {
            File dir = new File(savedPath);
            if (dir.exists() && dir.isDirectory()) {
                selectedDirectory = dir;
                pathLabel.setText(dir.getAbsolutePath());
                updateCodebaseStats(dir);
                
            }
        }
    }

    private void restoreSession() {
        List<ChatMessage> history = projectService.getChatHistory();
        String currentCache = projectService.getCurrentCacheName();
        if (history.isEmpty()) {
            if (currentCache != null) {
                addMessage("System", "Restoring codebase session...", false);
            } else {
                addMessage("System", "Select a folder and cache it to start chatting.", false);
            }
        } else {
            for (ChatMessage msg : history) {
                addMessage(msg.sender(), msg.text(), false);
            }
        }
        this.chat = projectService.getActiveChat();
        if (this.chat != null) {
            statusLabel.setText("Status: Ready");
        } else if (currentCache != null) {
            statusLabel.setText("Status: Reconnecting...");
            CompletableFuture.runAsync(() -> {
                try {
                    String model = projectService.getCurrentCacheModel();
                    if (model == null) model = geminiService.getPrefs().get(SettingsController.GEMINI_MODEL, SettingsController.DEFAULT_MODEL);
                    geminiService.validateCache(currentCache);
                    this.chat = geminiService.startChat(model, currentCache);
                    projectService.setActiveChat(this.chat);
                    Platform.runLater(() -> statusLabel.setText("Status: Ready"));
                } catch (Exception e) {
                    String msg = e.getMessage();
                    String userMsg = "Session expired. Please re-cache the codebase.";
                    if (msg != null) {
                        if (msg.contains("403")) {
                            userMsg = "Permission denied (403). Your API key may have changed or the cache is tied to another key. Please re-cache.";
                        } else if (msg.contains("404")) {
                            userMsg = "Cache not found (404). It may have expired. Please re-cache.";
                        }
                    }
                    final String finalMsg = userMsg;
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: Error");
                        addMessage("System", finalMsg, true);
                    });
                }
            });
        }
    }

    private void addMessage(String sender, String text, boolean save) {
        if (save) {
            projectService.addChatMessage(sender, text);
        }
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
                // e.printStackTrace();
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
            
            projectService.setProjectPath(dir.getAbsolutePath());
            updateCodebaseStats(dir);
        }
    }

    /**
     * Handles the cache codebase action.
     */



    @FXML
    private void handleSendMessage() {
        String input = chatInput.getText();
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        if (chat == null) {
            if (projectService.getCurrentCacheName() != null) {
                addMessage("System", "Still reconnecting to codebase session. Please wait a moment...", false);
            } else {
                addMessage("System", "Please select a folder and click 'Cache Codebase' before chatting.", false);
            }
            return;
        }
        chatInput.clear();
        addMessage("User", input, true);
        statusLabel.setText("Status: Thinking...");
        
        Agent selectedAgent = agentComboBox.getValue();
        int maxTurns = geminiService.getPrefs().getInt(SettingsController.CONVERSATION_MAX_TURNS, SettingsController.DEFAULT_MAX_TURNS);

        ChatLoop loop = new ChatLoop(chat, agentScriptService, maxTurns, new ChatLoop.Listener() {
            @Override
            public void onMessage(String sender, String text, boolean save) {
                Platform.runLater(() -> addMessage(sender, text, save));
            }

            @Override
            public void onStatus(String status) {
                Platform.runLater(() -> statusLabel.setText("Status: " + status));
            }

            @Override
            public void onTurn(int turn, int maxTurns) {
                Platform.runLater(() -> statusLabel.setText("Status: Turn " + turn + "/" + maxTurns));
            }

            @Override
            public void onComplete() {
                Platform.runLater(() -> statusLabel.setText("Status: Ready"));
            }

            @Override
            public void onError(Throwable error) {
                LOG.error("Error in handleSendMessage", error);
                String errorMessage = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
                
                String userFriendlyMessage = "Gemini Error: " + errorMessage;
                if (errorMessage.contains("403")) {
                    userFriendlyMessage = "Permission denied (403). Your API key may have changed or the cache is tied to another key. Please re-cache the codebase.";
                } else if (errorMessage.contains("404")) {
                    userFriendlyMessage = "Cache not found (404). It may have expired. Please re-cache the codebase.";
                }

                final String finalMsg = userFriendlyMessage;
                Platform.runLater(() -> {
                    addMessage("Error", finalMsg, true);
                    statusLabel.setText("Status: Error");
                });
            }
        });

        loop.run(input, selectedAgent);
    }




    private void updateCodebaseStats(File directory) {
        if (directory == null || !directory.exists()) {
            Platform.runLater(() -> {
                sizeLabel.setText("");
                tokenLabel.setText("");
            });
            return;
        }

        Platform.runLater(() -> {
            sizeLabel.setText("Size: Calculating...");
            tokenLabel.setText("Tokens: Calculating...");
        });

        CompletableFuture.runAsync(() -> {
            try {
                RepositoryScanner scanner = new RepositoryScanner();
                List<ProjectFile> files = scanner.scan(directory.toPath());
                XmlSmashFormatter formatter = new XmlSmashFormatter();
                String xml = formatter.format(files);
                
                long bytes = xml.length();
                int tokens = (int) (bytes / SettingsController.BYTES_PER_TOKEN);
                
                String sizeStr = formatSize(bytes);
                
                Platform.runLater(() -> {
                    sizeLabel.setText("Size: " + sizeStr);
                    tokenLabel.setText("Tokens: " + String.format("%, d", tokens));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    sizeLabel.setText("Size: Error");
                    tokenLabel.setText("Tokens: Error");
                });
                e.printStackTrace();
            }
        });
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

}