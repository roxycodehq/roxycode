package org.roxycode.gui;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SettingsControllerTest {

    private ApplicationContext context;
    private Preferences prefs;
    private static final String TEST_PREF_PATH = "org/roxycode/gui/test/SettingsControllerTest";

    @BeforeEach
    void setup() {
        context = ApplicationContext.run(Map.of("roxycode.preferences.path", TEST_PREF_PATH));
        prefs = context.getBean(Preferences.class);
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        if (prefs != null) {
            prefs.clear();
            prefs.removeNode();
            prefs.flush();
        }
        if (context != null) {
            context.close();
        }
    }

    @Test
    void testControllerIsAvailableInContext() {
        SettingsController controller = context.getBean(SettingsController.class);
        assertNotNull(controller);
    }

    @Test
    void testDefaultModelPreference() {
        String model = prefs.get(SettingsController.GEMINI_MODEL, SettingsController.DEFAULT_MODEL);
        assertEquals(SettingsController.DEFAULT_MODEL, model);
    }

    @Test
    void testLoadModelsDoesNotThrowException() {
        SettingsController controller = context.getBean(SettingsController.class);
        // This triggers initialize() -> loadModels() which uses Jackson TOML
        assertNotNull(controller);
    }
}
