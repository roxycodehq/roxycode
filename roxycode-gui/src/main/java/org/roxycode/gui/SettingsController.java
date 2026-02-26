package org.roxycode.gui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import io.micronaut.context.annotation.Prototype;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import jakarta.inject.Inject;
import java.util.prefs.Preferences;

@Prototype
public class SettingsController {

    public static final String GEMINI_API_KEY = "gemini_api_key";
    public static final String GEMINI_MODEL = "gemini_model";
    public static final String DEFAULT_MODEL = "models/gemini-2.5-flash";

    @Inject
    private Preferences prefs;
    @Inject
    private MainController mainController;


    @FXML
    private PasswordField apiKeyField;

    @FXML
    private ComboBox<GeminiModel> modelComboBox;

    @FXML
    private VBox modelDetailsBox;

    @FXML
    private Label modelDescriptionLabel;

    @FXML
    private Label modelPricingLabel;

    private Map<String, GeminiModel> models;

    @FXML
    public void initialize() {
        apiKeyField.setText(prefs.get(GEMINI_API_KEY, ""));
        loadModels();
        setupModelSelection();
    }

    private void loadModels() {
        TomlMapper mapper = new TomlMapper();
        try (InputStream is = getClass().getResourceAsStream("/models.toml")) {
            if (is == null) {
                throw new IOException("models.toml not found in resources");
            }
            models = mapper.readValue(is, new TypeReference<Map<String, GeminiModel>>() {});
            modelComboBox.getItems().addAll(models.values());
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback or error handling
        }
    }

    private void setupModelSelection() {
        String savedModelName = prefs.get(GEMINI_MODEL, DEFAULT_MODEL);
        
        GeminiModel selected = models.values().stream()
                .filter(m -> m.apiName().equals(savedModelName))
                .findFirst()
                .orElse(models.values().iterator().next());

        modelComboBox.setValue(selected);
        updateModelDetails(selected);

        modelComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateModelDetails(newVal);
            }
        });
    }

    private void updateModelDetails(GeminiModel model) {
        modelDescriptionLabel.setText(model.description());
        String pricing = String.format("Input: $%s/1M | Output: $%s/1M | Cached: $%s/1M",
                model.inputPrice(), model.outputPrice(), model.cachedPrice());
        modelPricingLabel.setText(pricing);
    }

    @FXML
    private void handleSave() {
        prefs.put(GEMINI_API_KEY, apiKeyField.getText());
        GeminiModel selected = modelComboBox.getValue();
        if (selected != null) {
            prefs.put(GEMINI_MODEL, selected.apiName());
        }
        mainController.showChat();
    }

    @FXML
    private void handleCancel() {
        mainController.showChat();
    }
}
