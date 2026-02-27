package org.roxycode.gui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service to manage and load available agents from configuration.
 */
@Singleton
public class AgentService {

    private Map<String, Agent> agents;

    @Inject
    public AgentService() {
        loadAgents();
    }

    private void loadAgents() {
        TomlMapper mapper = new TomlMapper();
        try (InputStream is = getClass().getResourceAsStream("/agents.toml")) {
            if (is == null) {
                throw new IOException("agents.toml not found in resources");
            }
            agents = mapper.readValue(is, new TypeReference<Map<String, Agent>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            agents = Map.of();
        }
    }

    /**
     * Returns all available agents.
     */
    public List<Agent> getAgents() {
        return new ArrayList<>(agents.values());
    }

    /**
     * Returns an agent by its ID.
     */
    public Agent getAgent(String id) {
        return agents.get(id);
    }
}
