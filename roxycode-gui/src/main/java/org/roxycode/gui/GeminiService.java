package org.roxycode.gui;

import com.google.genai.Client;
import com.google.genai.Chat;
import com.google.genai.types.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.prefs.Preferences;
import java.time.Duration;

/**
 * Service for interacting with Gemini API.
 */
@Singleton
public class GeminiService {

    @Inject
    private Preferences prefs;

    public Preferences getPrefs() {
        return prefs;
    }

    private Client client;

    private String lastUsedApiKey;

    String generateDisplayName(String folderName, String model) {
        String cleanModel = model.startsWith("models/") ? model.substring(7) : model;
        String userName = System.getProperty("user.name", "unknown");
        return "rc_" + userName + "_" + folderName + "_" + cleanModel;
    }

    /**
     * Returns the Gemini client, initializing it if necessary.
     * Recreates the client if the API key has changed in preferences.
     * @return the Gemini client
     */
    synchronized Client getClient() {
        String currentApiKey = prefs.get(SettingsController.GEMINI_API_KEY, "");
        if (client == null || !currentApiKey.equals(lastUsedApiKey)) {
            client = Client.builder().apiKey(currentApiKey).build();
            lastUsedApiKey = currentApiKey;
        }
        return client;
    }

    public CachedContent createCodebaseCache(String codebaseXml, String folderName) {
        String model = prefs.get(SettingsController.GEMINI_MODEL, SettingsController.DEFAULT_MODEL);
        String displayName = generateDisplayName(folderName, model);
        // Cleanup existing caches with the same display name
        listCaches().stream().filter(cache -> cache.displayName().isPresent() && displayName.equals(cache.displayName().get())).forEach(cache -> {
            try {
                getClient().caches.delete(cache.name().get(), DeleteCachedContentConfig.builder().build());
            } catch (Exception e) {
                System.err.println("Failed to delete old cache: " + cache.name().get() + " - " + e.getMessage());
            }
        });
        CreateCachedContentConfig config = CreateCachedContentConfig.builder().displayName(displayName).ttl(Duration.ofMinutes(prefs.getInt(SettingsController.CACHE_TTL_MINUTES, SettingsController.DEFAULT_TTL_MINUTES))).contents(Collections.singletonList(Content.builder().role("user").parts(Collections.singletonList(Part.builder().text(codebaseXml).build())).build())).tools(getTools()).build();
        return getClient().caches.create(model, config);
    }

    public Chat startChat(String model, String cachedContentName) {
        System.out.println("DEBUG: Starting chat session...");
        System.out.println("DEBUG: Model: " + model);
        System.out.println("DEBUG: Cache: " + cachedContentName);
        // Note: tools and system_instruction are NOT allowed in GenerateContentConfig
        // when using cachedContent. They must be set at cache creation time.
        GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder().cachedContent(cachedContentName);
        try {
            Chat chat = getClient().chats.create(model, configBuilder.build());
            System.out.println("DEBUG: Chat session created successfully.");
            return chat;
        } catch (Exception e) {
            System.err.println("DEBUG: Error creating chat session: " + e.getMessage());
            throw e;
        }
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

    /**
     * Deletes a cached content.
     * @param name the name of the cached content to delete
     */
    public void deleteCache(String name) {
        getClient().caches.delete(name, DeleteCachedContentConfig.builder().build());
    }

    /**
     * Retrieves a specific cached content by name.
     * @param name the resource name of the cache
     * @return the CachedContent
     */
    public CachedContent getCache(String name) {
        return getClient().caches.get(name, GetCachedContentConfig.builder().build());
    }

    private List<Tool> getTools() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("script", Schema.builder().type("STRING").description("The JavaScript code to execute").build());
        FunctionDeclaration executeJs = FunctionDeclaration.builder().name("execute_js").description("Executes JavaScript code in a sandbox and returns results as JSON.").parameters(Schema.builder().type("OBJECT").properties(properties).required(Collections.singletonList("script")).build()).build();
        return Collections.singletonList(Tool.builder().functionDeclarations(Collections.singletonList(executeJs)).build());
    }
}
