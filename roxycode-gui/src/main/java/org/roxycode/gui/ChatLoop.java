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

    /**
     * Runs the chat loop for a given user input and agent persona.
     * @param userInput the text entered by the user
     * @param agent the agent persona to use
     * @return a CompletableFuture that completes when the loop finishes
     */
    public CompletableFuture<Void> run(String userInput, Agent agent) {
        return CompletableFuture.runAsync(() -> {
            try {
                String currentInput = userInput;
                if (agent != null) {
                    currentInput = "Persona: " + agent.name() + "\n" +
                                   "Instructions: " + agent.systemPrompt() + "\n\n" +
                                   "User Query: " + userInput;
                }

                GenerateContentResponse response = chat.sendMessage(currentInput);

                for (int turn = 0; turn < maxTurns; turn++) {
                    int turnNum = turn + 1;
                    listener.onTurn(turnNum, maxTurns);

                    String text = response.text();
                    if (text != null && !text.trim().isEmpty()) {
                        listener.onMessage("Gemini", text, true);
                    }

                    List<FunctionCall> calls = response.functionCalls();
                    if (calls == null || calls.isEmpty()) break;

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

                            responseParts.add(Part.builder().functionResponse(
                                FunctionResponse.builder()
                                    .name("execute_js")
                                    .id(call.id().orElse(null))
                                    .response(responseMap)
                                    .build()
                            ).build());
                        }
                    }

                    if (responseParts.isEmpty()) break;

                    if (turn == maxTurns - 1) {
                        listener.onMessage("System", "Max turns (" + maxTurns + ") reached. Stopping further script execution.", false);
                        break;
                    }

                    response = chat.sendMessage(Content.builder().parts(responseParts).build());
                }
                listener.onComplete();
            } catch (Exception e) {
                LOG.error("Error in ChatLoop", e);
                listener.onError(e);
            }
        });
    }
}
