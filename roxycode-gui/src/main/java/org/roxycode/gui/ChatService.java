package org.roxycode.gui;

import com.google.genai.Chat;
import com.google.genai.ResponseStream;
import com.google.genai.types.*;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.roxycode.gui.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service to handle AI chat execution and broadcast domain events.
 */
@Singleton
public class ChatService {
    private static final Logger LOG = LoggerFactory.getLogger(ChatService.class);

    @Inject
    private ApplicationEventPublisher<Object> eventPublisher;

    @Inject
    private AgentScriptService agentScriptService;

    @ExecuteOn(TaskExecutors.BLOCKING)
    public void execute(String userInput, Agent agent, Chat chat, int maxTurns) {
        try {
            eventPublisher.publishEvent(new ChatStatusEvent("Thinking...", true));

            List<Content> history = agent.getHistory();
            
            // Prepare user input
            String currentInput = userInput;
            if (history.isEmpty()) {
                currentInput = "Persona: " + agent.getName() + "\n" +
                               "Instructions: " + agent.getSystemPrompt() + "\n\n" +
                               "User Query: " + userInput;
            }

            Content nextUserContent = Content.builder()
                    .role("user")
                    .parts(Collections.singletonList(Part.builder().text(currentInput).build()))
                    .build();

            for (int turn = 0; turn < maxTurns; turn++) {
                eventPublisher.publishEvent(new ChatStatusEvent("Turn " + (turn + 1) + "/" + maxTurns, true));
                
                // Add to agent history
                history.add(nextUserContent);
                trimHistory(history, agent.getMaxWindowSize());

                // Sync with SDK Chat history if possible. 
                // Since we can't easily set history on the SDK Chat object after creation,
                // we'll rely on the fact that sendMessage appends to it.
                // Trimming the SDK chat history is harder.
                
                ResponseStream stream = chat.sendMessageStream(nextUserContent);

                List<Part> aiParts = new ArrayList<>();
                List<FunctionCall> aggregateCalls = new ArrayList<>();
                processStream(stream, aggregateCalls, aiParts);
                
                // Add AI response to agent history
                Content modelContent = Content.builder().role("model").parts(aiParts).build();
                history.add(modelContent);

                if (aggregateCalls.isEmpty()) break;

                List<Part> responseParts = new ArrayList<>();
                for (FunctionCall call : aggregateCalls) {
                    String toolName = call.name().orElse("");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> args = (Map<String, Object>) call.args().orElse(Collections.emptyMap());
                    
                    eventPublisher.publishEvent(new ChatToolCallEvent(toolName, args));
                    
                    if ("execute_js".equals(toolName)) {
                        String script = (String) args.get("script");
                        eventPublisher.publishEvent(new ChatStatusEvent("Executing Script...", true));
                        String resultJson = agentScriptService.executeScript(script);
                        
                        Map<String, Object> responseMap = new HashMap<>();
                        responseMap.put("result", resultJson);
                        
                        FunctionResponse.Builder frBuilder = FunctionResponse.builder()
                                .name("execute_js")
                                .response(responseMap);
                        call.id().ifPresent(frBuilder::id);
                        responseParts.add(Part.builder().functionResponse(frBuilder.build()).build());
                    }
                }

                if (responseParts.isEmpty()) break;
                
                if (turn == maxTurns - 1) {
                    eventPublisher.publishEvent(new ChatContentEvent("\n*Max turns reached.*", true));
                    break;
                }

                // Prepare function responses as next user message
                nextUserContent = Content.builder().role("user").parts(responseParts).build();
            }

            eventPublisher.publishEvent(new ChatStatusEvent("Ready", false));

        } catch (Exception e) {
            LOG.error("Error in ChatService", e);
            eventPublisher.publishEvent(new ChatStatusEvent("Error: " + e.getMessage(), false));
        }
    }

    private void trimHistory(List<Content> history, int maxSize) {
        while (history.size() > maxSize) {
            history.remove(0);
        }
        // Ensure it starts with user role
        while (!history.isEmpty() && !"user".equals(history.get(0).role().orElse(""))) {
            history.remove(0);
        }
        // Ensure alternating roles (not strictly enforced here but removal usually happens in pairs)
    }

    private void processStream(ResponseStream stream, List<FunctionCall> aggregateCalls, List<Part> aiParts) {
        Iterator<Object> it = stream.iterator();
        while (it.hasNext()) {
            GenerateContentResponse response = (GenerateContentResponse) it.next();
            response.candidates().ifPresent(candidates -> {
                if (!candidates.isEmpty()) {
                    Candidate candidate = candidates.get(0);
                    candidate.content().ifPresent(content -> {
                        content.parts().ifPresent(parts -> {
                            for (Part part : parts) {
                                aiParts.add(part);
                                boolean isThought = part.thought().orElse(false);
                                part.text().ifPresent(text -> {
                                    if (isThought) {
                                        eventPublisher.publishEvent(new ChatThoughtEvent(text, false));
                                    } else {
                                        eventPublisher.publishEvent(new ChatContentEvent(text, false));
                                    }
                                });
                                part.functionCall().ifPresent(aggregateCalls::add);
                            }
                        });
                    });
                }
            });

            response.usageMetadata().ifPresent(usage -> {
                long prompt = usage.promptTokenCount().map(Number::longValue).orElse(0L);
                long candidates = usage.candidatesTokenCount().map(Number::longValue).orElse(0L);
                long cached = usage.cachedContentTokenCount().map(Number::longValue).orElse(0L);
                eventPublisher.publishEvent(new ChatUsageEvent(prompt, candidates, cached));
            });
        }
        
        eventPublisher.publishEvent(new ChatContentEvent("", true));
        eventPublisher.publishEvent(new ChatThoughtEvent("", true));
    }
}
