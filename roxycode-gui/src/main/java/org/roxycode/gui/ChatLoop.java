package org.roxycode.gui;

import com.google.genai.Chat;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Manages a multi-turn chat session with an agent, handling function calls and turn limits.
 */
public class ChatLoop {

    private static final Logger LOG = LoggerFactory.getLogger(ChatLoop.class);

    /**
     * Listener for ChatLoop events.
     */
    public interface Listener {

        void onMessage(String sender, String text, boolean save);

        void onStatus(String status);

        void onTurn(int turn, int maxTurns);

        void onComplete();

        void onError(Throwable error);
    }

    private final Chat chat;

    private final AgentScriptService agentScriptService;

    private final int maxTurns;

    private final Listener listener;

    public ChatLoop(Chat chat, AgentScriptService agentScriptService, int maxTurns, Listener listener) {
        this.chat = chat;
        this.agentScriptService = agentScriptService;
        this.maxTurns = maxTurns;
        this.listener = listener;
    }

    public CompletableFuture<Void> run(String userInput, Agent agent) {
        return CompletableFuture.runAsync(() -> {
            try {
                String currentInput = userInput;
                if (agent != null) {
                    currentInput = "Persona: " + agent.name() + "\n" + "Instructions: " + agent.systemPrompt() + "\n\n" + "User Query: " + userInput;
                }
                GenerateContentResponse response = chat.sendMessage(currentInput);
                for (int turn = 0; turn < maxTurns; turn++) {
                    int turnNum = turn + 1;
                    listener.onTurn(turnNum, maxTurns);
                    // Extract text parts manually to avoid SDK warnings about non-text parts
                    StringBuilder textBuilder = new StringBuilder();
                    response.candidates().ifPresent(candidates -> {
                        if (!candidates.isEmpty()) {
                            candidates.get(0).content().ifPresent(content -> {
                                content.parts().ifPresent(parts -> {
                                    for (Part part : parts) {
                                        part.text().ifPresent(textBuilder::append);
                                    }
                                });
                            });
                        }
                    });
                    String text = textBuilder.toString();
                    if (!text.trim().isEmpty()) {
                        listener.onMessage("Gemini", text, true);
                    }
                    List<FunctionCall> calls = response.functionCalls();
                    if (calls == null || calls.isEmpty())
                        break;
                    List<Part> responseParts = new ArrayList<>();
                    for (FunctionCall call : calls) {
                        if ("execute_js".equals(call.name().orElse(""))) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> args = (Map<String, Object>) call.args().orElse(Collections.emptyMap());
                            String script = (String) args.get("script");
                            listener.onStatus("Executing Script...");
                            String resultJson = agentScriptService.executeScript(script);
                            Map<String, Object> responseMap = new HashMap<>();
                            responseMap.put("result", resultJson);
                            FunctionResponse.Builder frBuilder = FunctionResponse.builder().name("execute_js").response(responseMap);
                            // Safely handle Optional ID to avoid NPE in Builder
                            call.id().ifPresent(frBuilder::id);
                            responseParts.add(Part.builder().functionResponse(frBuilder.build()).build());
                        }
                    }
                    if (responseParts.isEmpty())
                        break;
                    if (turn == maxTurns - 1) {
                        listener.onMessage("System", "Max turns (" + maxTurns + ") reached. Stopping further script execution.", false);
                        break;
                    }
                    response = chat.sendMessage(Content.builder().role("user").parts(responseParts).build());
                }
                listener.onComplete();
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg != null && (msg.contains("403") || msg.contains("404"))) {
                    LOG.error("Cache error detected in ChatLoop: {}. Possible session expiration or key mismatch.", msg);
                } else {
                    LOG.error("Error in ChatLoop", e);
                }
                listener.onError(e);
            }
        });
    }
}
