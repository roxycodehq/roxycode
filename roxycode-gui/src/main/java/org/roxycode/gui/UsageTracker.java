package org.roxycode.gui;

import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.roxycode.gui.events.ChatUsageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class UsageTracker {
    private static final Logger LOG = LoggerFactory.getLogger(UsageTracker.class);

    @Inject
    private ProjectService projectService;

    @EventListener
    public void onChatUsage(ChatUsageEvent event) {
        LOG.info("Usage reported: prompt={}, candidate={}, cached={}", 
            event.promptTokens(), event.candidateTokens(), event.cachedTokens());
        
        // In a real app, we might update a global counter or session costs
        // For now, let's just log it. 
        // We could potentially update ProjectService properties if we wanted it in the UI.
    }
}
