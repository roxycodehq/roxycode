package org.roxycode.gui;

import com.google.genai.types.Content;
import com.google.genai.types.Part;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class AgentHistoryLogicTest {

    @Test
    void testMergingConsecutiveParts() {
        Agent agent = new Agent.Builder()
                .id("test-agent")
                .name("Test Agent")
                .systemPrompt("Be helpful")
                .build();

        List<Content> history = agent.getHistory();
        
        // Turn 1: User message
        history.add(Content.builder()
                .role("user")
                .parts(List.of(Part.builder().text("Hello").build()))
                .build());

        // Turn 2: Model message with chunked parts
        history.add(Content.builder()
                .role("model")
                .parts(List.of(
                        Part.builder().text("I ").build(),
                        Part.builder().text("am ").build(),
                        Part.builder().text("a ").build(),
                        Part.builder().text("robot.").build()
                ))
                .build());

        List<AgentHistoryController.HistoryEntry> entries = AgentHistoryController.generateHistoryEntries(agent);

        // 1 (SYSTEM) + 1 (USER) + 1 (MODEL MERGED)
        assertEquals(3, entries.size());
        assertEquals("SYSTEM", entries.get(0).role());
        assertEquals("USER", entries.get(1).role());
        assertEquals("Hello", entries.get(1).content());
        assertEquals("MODEL", entries.get(2).role());
        assertEquals("I am a robot.", entries.get(2).content());
    }

    @Test
    void testMergingWithThoughts() {
        Agent agent = new Agent.Builder()
                .id("test-agent")
                .name("Test Agent")
                .systemPrompt("Be helpful")
                .build();

        List<Content> history = agent.getHistory();
        
        history.add(Content.builder()
                .role("model")
                .parts(List.of(
                        Part.builder().text("Thinking...").thought(true).build(),
                        Part.builder().text(" Still thinking...").thought(true).build(),
                        Part.builder().text("Hello!").thought(false).build()
                ))
                .build());

        List<AgentHistoryController.HistoryEntry> entries = AgentHistoryController.generateHistoryEntries(agent);

        // 1 (SYSTEM) + 1 (THOUGHT MERGED) + 1 (MODEL)
        assertEquals(3, entries.size());
        assertEquals("THOUGHT", entries.get(1).role());
        assertEquals("Thinking... Still thinking...", entries.get(1).content());
        assertEquals("MODEL", entries.get(2).role());
        assertEquals("Hello!", entries.get(2).content());
    }
}
