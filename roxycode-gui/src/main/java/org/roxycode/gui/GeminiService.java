package org.roxycode.gui;

import com.google.genai.Client;
import com.google.genai.Chat;
import com.google.genai.types.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.List;
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
     * @param folderName the name of the project folder
     * @return the created CachedContent
     */
    public CachedContent createCodebaseCache(String codebaseXml, String folderName) {
        String userName = System.getProperty("user.name", "unknown");
        String displayName = "rc_" + userName + "_" + folderName;

        // Cleanup existing caches with the same display name
        listCaches().stream()
            .filter(cache -> cache.displayName().isPresent() && displayName.equals(cache.displayName().get()))
            .forEach(cache -> {
                try {
                    getClient().caches.delete(cache.name().get(), DeleteCachedContentConfig.builder().build());
                } catch (Exception e) {
                    System.err.println("Failed to delete old cache: " + cache.name().get() + " - " + e.getMessage());
                }
            });

        String model = prefs.get(SettingsController.GEMINI_MODEL, SettingsController.DEFAULT_MODEL);
        CreateCachedContentConfig config = CreateCachedContentConfig.builder()
            .displayName(displayName)
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

    /**
     * Lists all cached contents.
     * @return a list of CachedContent objects
     */
    public List<CachedContent> listCaches() {
        com.google.genai.Pager<CachedContent> pager = getClient().caches.list(ListCachedContentsConfig.builder().build());
        java.util.List<CachedContent> list = new java.util.ArrayList<>();
        pager.forEach(list::add);
        return list;
    }
}
