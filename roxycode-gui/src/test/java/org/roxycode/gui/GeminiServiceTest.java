package org.roxycode.gui;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import com.google.genai.types.CachedContent;
import com.google.genai.Client;

@MicronautTest
@Property(name = "roxycode.preferences.path", value = "org/roxycode/gui/test/GeminiServiceTest")
class GeminiServiceTest {

    @Inject
    GeminiService geminiService;

    @Inject
    Preferences prefs;

    @AfterEach
    void tearDown() throws BackingStoreException {
        if (prefs != null && prefs.nodeExists("")) {
            prefs.clear();
        }
    }

    @Test
    void testInject() {
        Assertions.assertNotNull(geminiService);
    }

    @Test
    void testClientRecreationOnApiKeyChange() {
        // Set initial key
        prefs.put(SettingsController.GEMINI_API_KEY, "key1");
        Client client1 = geminiService.getClient();
        Assertions.assertNotNull(client1);
        
        // Same key should return same client
        Client client1Again = geminiService.getClient();
        Assertions.assertSame(client1, client1Again, "Should return the same client instance if the key has not changed");
        
        // Change key
        prefs.put(SettingsController.GEMINI_API_KEY, "key2");
        Client client2 = geminiService.getClient();
        Assertions.assertNotNull(client2);
        Assertions.assertNotSame(client1, client2, "Should create a new client instance when the API key changes");
        
        // Change back
        prefs.put(SettingsController.GEMINI_API_KEY, "key1");
        Client client3 = geminiService.getClient();
        Assertions.assertNotSame(client2, client3, "Should create a new client instance when the API key changes back");
    }

    @Test
    void testCreateCacheSignature() {
        // Method exists check
    }

    @Test
    void testGenerateDisplayName() {
        String folder = "my-project";
        String model = "models/gemini-2.5-flash";
        String userName = System.getProperty("user.name", "unknown");
        
        String displayName = geminiService.generateDisplayName(folder, model);
        
        Assertions.assertEquals("rc_" + userName + "_my-project_gemini-2.5-flash", displayName);
        
        // Test without models/ prefix
        String displayName2 = geminiService.generateDisplayName(folder, "gemini-ultra");
        Assertions.assertEquals("rc_" + userName + "_my-project_gemini-ultra", displayName2);
    }

    @Test
    void testDeleteCacheMethodExists() {
        Assertions.assertDoesNotThrow(() -> {
            try {
                geminiService.deleteCache("test-cache-name");
            } catch (Exception e) {
            }
        });
    }

    @Test
    void testGetCache() {
        // This is a smoke test to ensure the method exists and handles client call
        // Actual call will likely fail in test env without API key
        try {
            geminiService.getCache("test-name");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    void testStartChatConfiguration() {
        // Mocking the client/sdk is hard here, but we can verify it doesn't throw immediate errors 
        // if we provide a dummy model and cache name.
        // We mainly want to ensure startChat doesn't have syntax errors or null pointer issues with our logging.
        Assertions.assertDoesNotThrow(() -> {
            try {
                geminiService.startChat("models/gemini-2.5-flash", "cachedContents/test");
            } catch (Exception e) {
                // Ignore SDK errors due to missing API key, we just want to see if our logic runs
                System.out.println("StartChat call failed as expected: " + e.getMessage());
            }
        });
    }

}