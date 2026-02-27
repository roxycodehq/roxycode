package org.roxycode.gui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import io.micronaut.context.annotation.Prototype;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
    public static final String CACHE_MIN_TOKENS = "cache_min_tokens";
    public static final String CACHE_TTL_MINUTES = "cache_ttl_minutes";
    public static final String CONVERSATION_MAX_TURNS = "conversation_max_turns";

    public static final int DEFAULT_MIN_TOKENS = 4096;
    public static final int DEFAULT_TTL_MINUTES = 30;
    public static final int DEFAULT_MAX_TURNS = 5;
    public static final double BYTES_PER_TOKEN = 2.7;
    public static final String DEFAULT_MODEL = "models/gemini-2.5-flash";

    @Inject
    private Preferences prefs;
    @Inject
    private MainController mainController;
    @Inject
    private ProjectService projectService;


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

    @FXML
    private TextField minTokensField;

    @FXML
    private TextField ttlMinutesField;

    @FXML
    private TextField maxTurnsField;

    private Map<String, GeminiModel> models;

    @FXML
    public void initialize() {
        apiKeyField.setText(prefs.get(GEMINI_API_KEY, ""));
        minTokensField.setText(String.valueOf(prefs.getInt(CACHE_MIN_TOKENS, DEFAULT_MIN_TOKENS)));
        ttlMinutesField.setText(String.valueOf(prefs.getInt(CACHE_TTL_MINUTES, DEFAULT_TTL_MINUTES)));
        maxTurnsField.setText(String.valueOf(prefs.getInt(CONVERSATION_MAX_TURNS, DEFAULT_MAX_TURNS)));
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
        String oldKey = prefs.get(GEMINI_API_KEY, "");
        String oldModel = prefs.get(GEMINI_MODEL, DEFAULT_MODEL);
        String newKey = apiKeyField.getText();
        
        prefs.put(GEMINI_API_KEY, newKey);
        try {
            prefs.putInt(CACHE_MIN_TOKENS, Integer.parseInt(minTokensField.getText()));
            prefs.putInt(CACHE_TTL_MINUTES, Integer.parseInt(ttlMinutesField.getText()));
            prefs.putInt(CONVERSATION_MAX_TURNS, Integer.parseInt(maxTurnsField.getText()));
        } catch (NumberFormatException e) {
            // Use defaults if invalid
            prefs.putInt(CACHE_MIN_TOKENS, DEFAULT_MIN_TOKENS);
            prefs.putInt(CACHE_TTL_MINUTES, DEFAULT_TTL_MINUTES);
            prefs.putInt(CONVERSATION_MAX_TURNS, DEFAULT_MAX_TURNS);
        }
        GeminiModel selected = modelComboBox.getValue();
        String newModel = oldModel;
        if (selected != null) {
            newModel = selected.apiName();
            prefs.put(GEMINI_MODEL, newModel);
        }

        if (!oldKey.equals(newKey) || !oldModel.equals(newModel)) {
            projectService.clearSession();
        }
        mainController.showChat();
    }

    @FXML
    private void handleCancel() {
        mainController.showChat();
    }
}
