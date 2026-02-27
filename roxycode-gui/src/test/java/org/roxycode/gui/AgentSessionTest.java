package org.roxycode.gui;

import com.google.genai.types.Content;
import com.google.genai.types.Part;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgentSessionTest {

    @Test
    void testAgentBuilderAndHistory() {
        Agent agent = new Agent.Builder()
                .id("test-agent")
                .name("Test Agent")
                .role(AgentRole.RESEARCH)
                .maxWindowSize(4)
                .build();

        assertEquals("test-agent", agent.getId());
        assertEquals(AgentRole.RESEARCH, agent.getRole());
        assertNotNull(agent.getHistory());
        assertTrue(agent.getHistory().isEmpty());

        List<Content> history = agent.getHistory();
        history.add(createContent("user", "Hello"));
        history.add(createContent("model", "Hi"));
        history.add(createContent("user", "How are you?"));
        history.add(createContent("model", "I am fine"));
        
        assertEquals(4, agent.getHistory().size());
    }

    private Content createContent(String role, String text) {
        return Content.builder()
                .role(role)
                .parts(List.of(Part.builder().text(text).build()))
                .build();
    }
}
