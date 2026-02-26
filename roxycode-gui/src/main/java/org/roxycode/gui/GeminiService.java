package org.roxycode.gui;

import com.google.genai.Client;
import com.google.genai.Chat;
import com.google.genai.types.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.prefs.Preferences;
import java.time.Duration;

/**
 * Service for interacting with Gemini API.
 */
@Singleton
public class GeminiService {
    @Inject
    private Preferences prefs;

    private Client client;

    /**
     * Returns the Gemini client, initializing it if necessary.
     * @return the Gemini client
     */
    private synchronized Client getClient() {
        if (client == null) {
            String apiKey = prefs.get(SettingsController.GEMINI_API_KEY, "");
            client = Client.builder().apiKey(apiKey).build();
        }
        return client;
    }

    /**
     * Creates a codebase cache with a 30-minute TTL.
     * @param codebaseXml the codebase XML content
     * @return the created CachedContent
     */
    public CachedContent createCodebaseCache(String codebaseXml) {
        String model = prefs.get(SettingsController.GEMINI_MODEL, SettingsController.DEFAULT_MODEL);
        CreateCachedContentConfig config = CreateCachedContentConfig.builder()
            .ttl(Duration.ofMinutes(30))
            .contents(Collections.singletonList(
                Content.builder()
                    .role("user")
                    .parts(Collections.singletonList(Part.builder().text(codebaseXml).build()))
                    .build()
            ))
            .build();
        return getClient().caches.create(model, config);
    }

    /**
     * Starts a chat using a cached codebase.
     * @param cachedContentName the name of the cached content
     * @return the Chat instance
     */
    public Chat startChat(String cachedContentName) {
        String model = prefs.get(SettingsController.GEMINI_MODEL, SettingsController.DEFAULT_MODEL);
        GenerateContentConfig config = GenerateContentConfig.builder()
            .cachedContent(cachedContentName)
            .build();
        return getClient().chats.create(model, config);
    }
}
