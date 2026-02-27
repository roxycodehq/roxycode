package org.roxycode.gui;

import com.google.genai.Chat;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Service to manage and persist the currently active project path and session state.
 */
@Singleton
public class ProjectService {

    public static final String CURRENT_PROJECT_KEY = "current_project_path";

    public static final String CURRENT_CACHE_KEY = "current_cache_name";

    public static final String CURRENT_CACHE_MODEL_KEY = "current_cache_model";

    public static final String ROXY_DIR = "roxy";

    public static final String CONFIG_DIR = "config";

    public static final String CACHE_DIR = "cache";

    public static final String CACHE_FILE = "codebase_cache.xml";

    private final StringProperty projectPath = new SimpleStringProperty();

    private final Preferences prefs;

    // System Information Properties
    private final StringProperty osName = new SimpleStringProperty(System.getProperty("os.name"));

    private final StringProperty userName = new SimpleStringProperty(System.getProperty("user.name"));

    private final StringProperty javaVersion = new SimpleStringProperty(System.getProperty("java.version"));

    private final StringProperty gitBranch = new SimpleStringProperty("N/A");

    // Local Cache Metadata Properties
    private final StringProperty localCachePath = new SimpleStringProperty("-");

    private final StringProperty localCacheSize = new SimpleStringProperty("-");

    private final StringProperty localCacheTime = new SimpleStringProperty("-");

    private final StringProperty localCacheTokens = new SimpleStringProperty("-");

    // Session state
    private Chat activeChat;

    private final List<ChatMessage> chatHistory = new ArrayList<>();

    private final StringProperty currentCacheName = new SimpleStringProperty();

    private final StringProperty currentCacheModel = new SimpleStringProperty();

    @Inject
    public ProjectService(Preferences prefs) {
        this.prefs = prefs;
        // Load initial values
        String savedPath = prefs.get(CURRENT_PROJECT_KEY, null);
        projectPath.set(savedPath);
        this.currentCacheName.set(prefs.get(CURRENT_CACHE_KEY, null));
        this.currentCacheModel.set(prefs.get(CURRENT_CACHE_MODEL_KEY, null));
        if (savedPath != null) {
            updateGitBranch(savedPath);
            refreshLocalCacheInfo();
        }
        // Save when changed and clear session
        projectPath.addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                prefs.remove(CURRENT_PROJECT_KEY);
            } else {
                prefs.put(CURRENT_PROJECT_KEY, newVal);
            }
            // Clear session if project changes
            if (oldVal == null || !oldVal.equals(newVal)) {
                clearSession();
            }
            updateGitBranch(newVal);
            refreshLocalCacheInfo();
        });
    }

    private void updateGitBranch(String path) {
        if (path == null || path.isEmpty()) {
            safeSetProperty(gitBranch, "N/A");
            return;
        }
        new Thread(() -> {
            try {
                File gitDir = new File(path, ".git");
                if (!gitDir.exists()) {
                    // Try to find parent .git if the selected path is a subfolder
                    File current = new File(path);
                    while (current != null && !new File(current, ".git").exists()) {
                        current = current.getParentFile();
                    }
                    if (current != null) {
                        gitDir = new File(current, ".git");
                    }
                }
                if (gitDir.exists()) {
                    try (Repository repository = new FileRepositoryBuilder().setGitDir(gitDir).readEnvironment().findGitDir().build()) {
                        String branch = repository.getBranch();
                        safeSetProperty(gitBranch, branch);
                    }
                } else {
                    safeSetProperty(gitBranch, "N/A");
                }
            } catch (IOException e) {
                safeSetProperty(gitBranch, "Error");
            }
        }).start();
    }

    public void refreshLocalCacheInfo() {
        String pathStr = getProjectPath();
        if (pathStr == null) {
            safeSetProperty(localCachePath, "Not found");
            safeSetProperty(localCacheSize, "-");
            safeSetProperty(localCacheTime, "-");
            safeSetProperty(localCacheTokens, "-");
            return;
        }
        Path cachePath = Paths.get(pathStr, ROXY_DIR, CACHE_DIR, CACHE_FILE);
        File cacheFile = cachePath.toFile();
        if (cacheFile.exists()) {
            long bytes = cacheFile.length();
            String sizeStr = formatSize(bytes);
            int estimatedTokens = (int) (bytes / SettingsController.BYTES_PER_TOKEN);
            String tokensStr = String.format("%,d", estimatedTokens);
            try {
                BasicFileAttributes attrs = Files.readAttributes(cachePath, BasicFileAttributes.class);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
                String timeStr = formatter.format(attrs.creationTime().toInstant());
                safeSetProperty(localCachePath, cacheFile.getAbsolutePath());
                safeSetProperty(localCacheSize, sizeStr);
                safeSetProperty(localCacheTime, timeStr);
                safeSetProperty(localCacheTokens, tokensStr);
            } catch (IOException e) {
                safeSetProperty(localCachePath, cacheFile.getAbsolutePath());
                safeSetProperty(localCacheTime, "Error reading attributes");
            }
        } else {
            safeSetProperty(localCachePath, "Not found");
            safeSetProperty(localCacheSize, "-");
            safeSetProperty(localCacheTime, "-");
            safeSetProperty(localCacheTokens, "-");
        }
    }

    private void safeSetProperty(StringProperty prop, String value) {
        try {
            if (Platform.isFxApplicationThread()) {
                prop.set(value);
            } else {
                Platform.runLater(() -> prop.set(value));
            }
        } catch (IllegalStateException e) {
            // Toolkit not initialized - fallback to direct set for headless tests
            prop.set(value);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

    public StringProperty projectPathProperty() {
        return projectPath;
    }

    public String getProjectPath() {
        return projectPath.get();
    }

    public void setProjectPath(String path) {
        this.projectPath.set(path);
    }

    public StringProperty osNameProperty() {
        return osName;
    }

    public StringProperty userNameProperty() {
        return userName;
    }

    public StringProperty javaVersionProperty() {
        return javaVersion;
    }

    public StringProperty gitBranchProperty() {
        return gitBranch;
    }

    public StringProperty localCachePathProperty() {
        return localCachePath;
    }

    public StringProperty localCacheSizeProperty() {
        return localCacheSize;
    }

    public StringProperty localCacheTimeProperty() {
        return localCacheTime;
    }

    public StringProperty localCacheTokensProperty() {
        return localCacheTokens;
    }

    public Chat getActiveChat() {
        return activeChat;
    }

    public void setActiveChat(Chat chat) {
        this.activeChat = chat;
    }

    public List<ChatMessage> getChatHistory() {
        return chatHistory;
    }

    public void addChatMessage(String sender, String text) {
        chatHistory.add(new ChatMessage(sender, text));
    }

    public String getCurrentCacheName() {
        return currentCacheName.get();
    }

    public StringProperty currentCacheNameProperty() {
        return currentCacheName;
    }

    public void setCurrentCacheName(String cacheName) {
        this.currentCacheName.set(cacheName);
        if (cacheName == null) {
            prefs.remove(CURRENT_CACHE_KEY);
        } else {
            prefs.put(CURRENT_CACHE_KEY, cacheName);
        }
    }

    public void clearSession() {
        this.activeChat = null;
        this.chatHistory.clear();
        this.currentCacheName.set(null);
        this.currentCacheModel.set(null);
        prefs.remove(CURRENT_CACHE_KEY);
        prefs.remove(CURRENT_CACHE_MODEL_KEY);
    }

    public String getCurrentCacheModel() {
        return currentCacheModel.get();
    }

    public StringProperty currentCacheModelProperty() {
        return currentCacheModel;
    }

    public void setCurrentCacheModel(String modelName) {
        this.currentCacheModel.set(modelName);
        if (modelName == null) {
            prefs.remove(CURRENT_CACHE_MODEL_KEY);
        } else {
            prefs.put(CURRENT_CACHE_MODEL_KEY, modelName);
        }
    }
}
