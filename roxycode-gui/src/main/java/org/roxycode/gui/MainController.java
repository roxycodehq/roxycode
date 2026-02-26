package org.roxycode.gui;

import io.micronaut.context.ApplicationContext;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import jakarta.inject.Inject;
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
    private StackPane contentArea;

    @FXML
    private Button chatNavButton;

    @FXML
    private Button settingsNavButton;

    @FXML
    private Button cachesNavButton;

    @Inject
    private ApplicationContext context;

    /**
     * Initializes the controller.
     */
    public void initialize() {
        userLabel.setText("User: " + System.getProperty("user.name"));
        projectLabel.setText("Project: RoxyCode");
        showChat();
    }

    /**
     * Shows the codebase chat view in the content area.
     */
    @FXML
    public void showChat() {
        updateActiveNavButton(chatNavButton);
        loadView("/org/roxycode/gui/codebase-chat.fxml");
    }

    /**
     * Shows the settings view in the content area.
     */
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
}
