package org.roxycode.gui;

/**
 * Represents a single message in the chat history.
 */
public record ChatMessage(String sender, String text) {
}
