package org.roxycode.gui;

import io.micronaut.context.ApplicationContext;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.application.Platform;
import com.google.genai.types.CachedContent;
import java.time.Instant;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import javafx.scene.layout.StackPane;

import jakarta.inject.Inject;
import javafx.beans.binding.Bindings;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for the main view of the application.
 */
@Singleton
public class MainController {

    @FXML
    private Label userLabel;
    
    @FXML
    private Label projectLabel;

    @FXML
    private Label osLabel;

    @FXML
    private Label javaLabel;

    @FXML
    private Label branchLabel;

    @FXML
    private Label cacheStatusLabel;

    @FXML
    private StackPane contentArea;

    @FXML
    private Button chatNavButton;

    @FXML
    private Button settingsNavButton;

    @FXML
    private Button cachesNavButton;

    @Inject
    private ApplicationContext context;

    @Inject
    private ProjectService projectService;

    /**
     * Initializes the controller.
     */
    public void initialize() {
        userLabel.textProperty().bind(Bindings.concat("User: ", projectService.userNameProperty()));
        osLabel.textProperty().bind(projectService.osNameProperty());
        javaLabel.textProperty().bind(projectService.javaVersionProperty());
        branchLabel.textProperty().bind(projectService.gitBranchProperty());
        
        projectLabel.textProperty().bind(Bindings.createStringBinding(
            () -> {
                String path = projectService.getProjectPath();
                if (path == null || path.isEmpty()) {
                    return "No Project Selected";
                }
                // Just show the directory name for cleaner UI
                java.io.File file = new java.io.File(path);
                return "Project: " + file.getName();
            },
            projectService.projectPathProperty()
        ));
        
        projectService.currentCacheNameProperty().addListener((obs, oldVal, newVal) -> updateCacheStatus());
        startStatusRefreshLoop();
        updateCacheStatus();
        showChat();
    }

    /**
     * Shows the codebase chat view in the content area.
     */
    @FXML
    public void showCacheDetails() {
        loadView("/org/roxycode/gui/cache-details.fxml");
    }

    public void showChat() {
        updateActiveNavButton(chatNavButton);
        loadView("/org/roxycode/gui/codebase-chat.fxml");
    }

    /**
     * Shows the caches view in the content area.
     */
    @FXML
    private void showCaches() {
        updateActiveNavButton(cachesNavButton);
        loadView("/org/roxycode/gui/cache-list.fxml");
    }

    @FXML
    private void showSettings() {
        updateActiveNavButton(settingsNavButton);
        loadView("/org/roxycode/gui/settings.fxml");
    }

    private void updateActiveNavButton(Button activeBtn) {
        List<Button> buttons = Arrays.asList(chatNavButton, cachesNavButton, settingsNavButton);
        for (Button btn : buttons) {
            btn.getStyleClass().remove("active");
        }
        activeBtn.getStyleClass().add("active");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(context::getBean);
            Node view = loader.load();
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            contentArea.getChildren().setAll(new Label("Error loading view: " + e.getMessage()));
        }
    }

    @Inject
    private GeminiService geminiService;

    private void updateCacheStatus() {
        String cacheName = projectService.getCurrentCacheName();
        if (cacheName == null) {
            Platform.runLater(() -> cacheStatusLabel.setText("Cache: None"));
            return;
        }

        new Thread(() -> {
            try {
                CachedContent cache = geminiService.getCache(cacheName);
                String tokens = cache.usageMetadata().flatMap(um -> um.totalTokenCount()).map(t -> t + " tokens").orElse("? tokens");
                String ttlStr = cache.expireTime().map(et -> {
                    Duration d = Duration.between(Instant.now(), Instant.parse(et.toString()));
                    long mins = d.toMinutes();
                    return mins > 0 ? mins + "m left" : "Expired";
                }).orElse("Unknown");

                Platform.runLater(() -> cacheStatusLabel.setText(String.format("Cache: Active (%s, %s)", tokens, ttlStr)));
            } catch (Exception e) {
                Platform.runLater(() -> cacheStatusLabel.setText("Cache: Error/Expired"));
            }
        }).start();
    }

    private void startStatusRefreshLoop() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateCacheStatus();
            }
        }, 60000, 60000); // Every minute
    }

}
