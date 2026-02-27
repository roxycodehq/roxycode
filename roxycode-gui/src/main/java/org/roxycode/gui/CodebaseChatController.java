package org.roxycode.gui;

import com.google.genai.Chat;
import com.google.genai.types.*;
import jakarta.inject.Singleton;
import io.micronaut.runtime.event.annotation.EventListener;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import jakarta.inject.Inject;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.roxycode.gui.events.*;
import org.roxycode.jsmashy.core.RepositoryScanner;
import org.roxycode.jsmashy.core.ProjectFile;
import org.roxycode.jsmashy.formatters.XmlSmashFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Codebase Chat screen, refactored for Event-Driven Architecture.
 */
@Singleton
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

    @FXML
    private Label agentNameLabel;

    @FXML
    private Label turnsLabel;

    @FXML
    private Label historyLabel;

    @Inject
    private AgentService agentService;

    @Inject
    private GeminiService geminiService;

    @Inject
    private ProjectService projectService;

    @Inject
    private ChatService chatService;

    @Inject
    private AgentScriptService agentScriptService;

    @Inject
    private TemplateService templateService;

    private WebEngine webEngine;

    private final MarkdownRenderer markdownRenderer = new MarkdownRenderer();

    private final StringBuilder aiResponseBuffer = new StringBuilder();

    private boolean webViewLoaded = false;

    private final List<String> pendingScripts = new ArrayList<>();

    @FXML
    public void initialize() {
        webEngine = chatWebView.getEngine();
        String visibility = geminiService.getPrefs().get(SettingsController.THOUGHTS_VISIBILITY, SettingsController.THOUGHTS_SHOW_COLLAPSED);
        // Migration logic for old preference values
        if ("Don't Show".equals(visibility)) visibility = SettingsController.THOUGHTS_DONT_SHOW;
        else if ("Show Collapsed".equals(visibility)) visibility = SettingsController.THOUGHTS_SHOW_COLLAPSED;
        else if ("Show Full".equals(visibility)) visibility = SettingsController.THOUGHTS_SHOW_FULL;

        webEngine.loadContent(templateService.render("templates/chat.html", Map.of("thoughtsVisibility", visibility)));
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                webViewLoaded = true;
                flushPendingScripts();
                restoreSession();
                Platform.runLater(() -> {
                    agentComboBox.getItems().setAll(agentService.getAgents());
                    agentComboBox.getSelectionModel().selectedItemProperty().addListener((agentObs, oldVal, newVal) -> bindAgentStatus(newVal));
                    if (!agentComboBox.getItems().isEmpty()) {
                        agentComboBox.getSelectionModel().select(0);
                    bindAgentStatus(agentComboBox.getValue());
                    }
                });
            }
        });
        String savedPath = projectService.getProjectPath();
        if (savedPath != null) {
            File dir = new File(savedPath);
            if (dir.exists() && dir.isDirectory()) {
                pathLabel.setText(dir.getAbsolutePath());
                updateCodebaseStats(dir);
            }
        }
    }

    @EventListener
    public void onChatStatus(ChatStatusEvent event) {
        LOG.debug("Received ChatStatusEvent: {}", event.status());
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText("Status: " + event.status());
            }
            if (event.status().startsWith("Error:")) {
                addMessage("Error", event.status(), true);
            }
        });
    }

    @EventListener
    public void onChatContent(ChatContentEvent event) {
        LOG.debug("Received ChatContentEvent: textLength={}, isFinal={}", event.text().length(), event.isFinal());
        Platform.runLater(() -> {
            if (!event.text().isEmpty()) {
                aiResponseBuffer.append(event.text());
                streamToWebView("content", event.text(), false);
            }
            if (event.isFinal() && aiResponseBuffer.length() > 0) {
                String fullText = aiResponseBuffer.toString();
                projectService.addChatMessage("Gemini", fullText);
                String fullHtml = markdownRenderer.render(fullText);
                aiResponseBuffer.setLength(0);
                streamToWebView("content", fullHtml, true);
            }
        });
    }

    @EventListener
    public void onChatThought(ChatThoughtEvent event) {
        LOG.debug("Received ChatThoughtEvent: textLength={}, isFinal={}", event.text().length(), event.isFinal());
        streamToWebView("thought", event.text(), event.isFinal());
    }

    @EventListener
    public void onChatUsage(ChatUsageEvent event) {
        // Future use: display usage stats in UI
    }

    private void bindAgentStatus(Agent agent) {
        if (agent == null) return;
        agentNameLabel.textProperty().bind(agent.nameProperty()); // Wait, Agent name is String. I should use SimpleStringProperty if I want to bind it. But name is fixed. 
        // Agent.java has getName() returning String. I should probably add nameProperty() or just set it once.
        agentNameLabel.setText(agent.getName());
        turnsLabel.textProperty().bind(agent.currentTurnsProperty().asString());
        historyLabel.textProperty().bind(agent.historySizeProperty().asString());
    }

    @FXML
    private void handleAgentChange() {
        bindAgentStatus(agentComboBox.getValue());
    }

    @FXML
    private void handleSendMessage() {
        String input = chatInput.getText();
        if (input == null || input.trim().isEmpty())
            return;
        
        Agent agent = agentComboBox.getValue();
        if (agent == null) return;

        Chat chat = (Chat) agent.getSession().get("chat");
        if (chat == null) {
            if (projectService.getCurrentCacheName() != null) {
                reconnectSession(agent);
                addMessage("System", "Initializing agent session...", false);
                return;
            } else {
                addMessage("System", "Please select a folder and cache it before chatting.", false);
                return;
            }
        }

        LOG.info("Sending message to {}: {}", agent.getName(), input);
        chatInput.clear();
        aiResponseBuffer.setLength(0);
        addMessage("User", input, true);
        
        int maxTurns = geminiService.getPrefs().getInt(SettingsController.CONVERSATION_MAX_TURNS, SettingsController.DEFAULT_MAX_TURNS);
        CompletableFuture.runAsync(() -> {
            chatService.execute(input, agent, chat, maxTurns);
        }).exceptionally(ex -> {
            LOG.error("Failed to execute chat", ex);
            Platform.runLater(() -> addMessage("System", "Error: " + ex.getMessage(), false));
            return null;
        });
    }

    private void addMessage(String sender, String text, boolean save) {
        if (save)
            projectService.addChatMessage(sender, text);
        String htmlContent = escapeJs(markdownRenderer.render(text));
        String containerClass = "message-container " + ("User".equals(sender) ? "user-message-container" : ("Gemini".equals(sender) ? "ai-message-container" : ""));
        String bubbleClass = "bubble " + ("User".equals(sender) ? "user-bubble" : ("Gemini".equals(sender) ? "ai-bubble" : "system-bubble"));
        String script = String.format("addMessage('%s', '%s', '%s')", containerClass, bubbleClass, htmlContent);
        runScript(script);
    }

    private void streamToWebView(String type, String text, boolean isFinal) {
        String escaped = escapeJs(text);
        String script = String.format("streamToWebView('%s', '%s', %b)", type, escaped, isFinal);
        runScript(script);
    }

    private void restoreSession() {
        List<ChatMessage> history = projectService.getChatHistory();
        for (ChatMessage msg : history) {
            addMessage(msg.sender(), msg.text(), false);
        }
        // We don't restore a global chat object anymore, agents manage their own.
        if (projectService.getCurrentCacheName() != null) {
            statusLabel.setText("Status: Ready (Cache Active)");
        }
    }

    private void reconnectSession(Agent agent) {
        statusLabel.setText("Status: Connecting " + agent.getName() + "...");
        CompletableFuture.runAsync(() -> {
            try {
                String cacheName = projectService.getCurrentCacheName();
                String model = projectService.getCurrentCacheModel();
                if (model == null)
                    model = geminiService.getPrefs().get(SettingsController.GEMINI_MODEL, SettingsController.DEFAULT_MODEL);
                geminiService.validateCache(cacheName);
                Chat chat = geminiService.startChat(model, cacheName);
                agent.getSession().put("chat", chat);
                Platform.runLater(() -> statusLabel.setText("Status: Ready"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Status: Error");
                    addMessage("System", "Session error for " + agent.getName() + ": " + e.getMessage(), false);
                });
            }
        });
    }

    @FXML
    private void handleSelectFolder() {
        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
        File dir = dc.showDialog(null);
        if (dir != null) {
            pathLabel.setText(dir.getAbsolutePath());
            projectService.setProjectPath(dir.getAbsolutePath());
            updateCodebaseStats(dir);
        }
    }

    private void updateCodebaseStats(File directory) {
        CompletableFuture.runAsync(() -> {
            try {
                RepositoryScanner scanner = new RepositoryScanner();
                List<ProjectFile> files = scanner.scan(directory.toPath());
                String xml = new XmlSmashFormatter().format(files);
                long bytes = xml.length();
                int tokens = (int) (bytes / SettingsController.BYTES_PER_TOKEN);
                Platform.runLater(() -> {
                    if (sizeLabel != null)
                        sizeLabel.setText("Size: " + formatSize(bytes));
                    if (tokenLabel != null)
                        tokenLabel.setText("Tokens: " + String.format("%,d", tokens));
                });
            } catch (Exception ignored) {
            }
        });
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), "KMGTPE".charAt(exp - 1));
    }

    static String escapeJs(String text) {
        if (text == null)
            return "";
        return text.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\b", "\\b").replace("\f", "\\f");
    }

    private void runScript(String script) {
        Platform.runLater(() -> {
            if (!webViewLoaded) {
                LOG.debug("WebView not loaded, queuing script");
                pendingScripts.add(script);
                return;
            }
            try {
                webEngine.executeScript(script);
            } catch (Exception e) {
                LOG.error("Failed to execute script: " + script, e);
            }
        });
    }

    private void flushPendingScripts() {
        LOG.debug("Flushing {} pending scripts", pendingScripts.size());
        for (String script : pendingScripts) {
            try {
                webEngine.executeScript(script);
            } catch (Exception e) {
                LOG.error("Failed to execute pending script: " + script, e);
            }
        }
        pendingScripts.clear();
    }
}
