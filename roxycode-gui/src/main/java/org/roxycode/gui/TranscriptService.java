package org.roxycode.gui;

import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.roxycode.gui.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service to archive chat events to markdown transcript files.
 */
@Singleton
public class TranscriptService {
    private static final Logger LOG = LoggerFactory.getLogger(TranscriptService.class);

    @Inject
    private ProjectService projectService;

    private Path currentTranscript;

    @EventListener
    public void onStatus(ChatStatusEvent event) {
        // Create a new transcript when a new "Thinking..." state starts
        if ("Thinking...".equals(event.status())) {
            ensureTranscriptFile();
        }
    }

    @EventListener
    public void onContent(ChatContentEvent event) {
        if (!event.text().isEmpty()) {
            appendTranscript(event.text());
        }
        if (event.isFinal()) {
            appendTranscript("\n\n");
        }
    }

    @EventListener
    public void onThought(ChatThoughtEvent event) {
        if (!event.text().isEmpty()) {
            appendTranscript("> " + event.text().replace("\n", "\n> "));
        }
        if (event.isFinal()) {
            appendTranscript("\n\n");
        }
    }

    @EventListener
    public void onToolCall(ChatToolCallEvent event) {
        appendTranscript("\n* **Tool Call**: " + event.toolName() + " with " + event.arguments() + "\n\n");
    }

    private synchronized void ensureTranscriptFile() {
        String projectPath = projectService.getProjectPath();
        if (projectPath == null) return;

        Path archiveDir = Paths.get(projectPath, "roxy", "plans", "archive");
        try {
            Files.createDirectories(archiveDir);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            currentTranscript = archiveDir.resolve("transcript_" + timestamp + ".md");
            Files.writeString(currentTranscript, "# Chat Transcript - " + timestamp + "\n\n", StandardOpenOption.CREATE);
        } catch (IOException e) {
            LOG.error("Failed to create transcript file", e);
        }
    }

    private synchronized void appendTranscript(String text) {
        if (currentTranscript == null) return;
        try {
            Files.writeString(currentTranscript, text, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("Failed to append to transcript", e);
        }
    }
}
