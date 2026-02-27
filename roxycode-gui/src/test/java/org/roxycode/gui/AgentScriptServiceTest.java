package org.roxycode.gui;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@MicronautTest
class AgentScriptServiceTest {

    @Inject
    AgentScriptService scriptService;

    @Test
    void testExecuteScript() throws Exception {
        String script = "console.log('Hello'); 1 + 1;";
        String json = scriptService.executeScript(script);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        
        assertTrue(node.get("success").asBoolean(), "Execution should be successful");
        assertEquals(2, node.get("returnValue").asInt(), "Return value should be 2");
        assertTrue(node.get("logs").asText().contains("Hello"), "Logs should contain 'Hello'");
    }

    @Test
    void testErrorHandling() throws Exception {
        String script = "throw new Error('test error');";
        String json = scriptService.executeScript(script);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        
        assertFalse(node.get("success").asBoolean(), "Execution should fail");
        assertTrue(node.get("errorMessage").asText().contains("test error"), "Error message should be present");
    }
    @Test
    void testNullScript() throws Exception {
        String json = scriptService.executeScript(null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        
        assertTrue(node.get("success").asBoolean(), "Execution of null script should be successful (treated as empty)");
        assertEquals("null", node.get("returnValue").asText(), "Return value should be null string");
    }

    @Test
    void testUndefinedReturn() throws Exception {
        String json = scriptService.executeScript("var x; x;");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        
        assertTrue(node.get("success").asBoolean(), "Execution should be successful");
        assertEquals("null", node.get("returnValue").asText(), "Undefined return should be mapped to null string");
    }
}