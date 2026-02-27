package org.roxycode.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.HashMap;

/**
 * Service to execute JavaScript code in a GraalJS sandbox.
 */
@Singleton
public class AgentScriptService {
    private static final Logger LOG = LoggerFactory.getLogger(AgentScriptService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private PlanService planService;

    @Inject
    private ProjectService projectService;

    @Inject
    private AgentService agentService;

    /**
     * Executes the provided JavaScript code and returns a JSON string with the result.
     * @param script the JavaScript code to execute
     * @return a JSON string containing returnValue, logs, success, and errorMessage
     */
    public String executeScript(String script) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        
        if (script == null) {
            script = "";
        }

        try (Context context = Context.newBuilder("js")
                .out(ps)
                .err(ps)
                .allowAllAccess(true)
                .build()) {
            
            var bindings = context.getBindings("js");
            bindings.putMember("planService", planService);
            bindings.putMember("projectService", projectService);
            bindings.putMember("agentService", agentService);
            
            Value result = context.eval("js", script);
            Object returnValue = result != null ? (result.isHostObject() ? result.asHostObject() : result.as(Object.class)) : null;
            
            Map<String, Object> response = new HashMap<>();
            response.put("returnValue", returnValue != null ? returnValue : "null");
            response.put("logs", out.toString());
            response.put("success", true);
            response.put("errorMessage", "");
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            LOG.error("Error executing JavaScript", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("returnValue", "null");
            errorResponse.put("logs", out.toString());
            errorResponse.put("success", false);
            errorResponse.put("errorMessage", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            
            try {
                return objectMapper.writeValueAsString(errorResponse);
            } catch (Exception ex) {
                LOG.error("Error running js", ex);
                return "{\"success\": false, \"errorMessage\": \"Serialization error: \" + ex.getMessage() + \"\"}";
            }
        }
    }
}
