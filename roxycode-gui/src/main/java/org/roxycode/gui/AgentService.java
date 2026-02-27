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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to manage and load available agents from configuration.
 */
@Singleton
public class AgentService {
    private static final Logger LOG = LoggerFactory.getLogger(AgentService.class);

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
            agents = mapper.readValue(is, new TypeReference<Map<String, Agent>>() {
            });
        } catch (IOException e) {
            LOG.error("Unable to load agents", e);
            agents = Map.of();
        }
    }

    /**
     * Returns all avai
     * @return lable agents.
     */
    public List<Agent> getAgents() {
        return new ArrayList<>(agents.values());
    }

    /**
     * Returns an agent by its ID.
     * @param id
     * @return 
     */
    public Agent getAgent(String id) {
        return agents.get(id);
    }
}
