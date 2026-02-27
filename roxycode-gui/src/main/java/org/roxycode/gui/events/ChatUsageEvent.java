package org.roxycode.gui.events;

public record ChatUsageEvent(long promptTokens, long candidateTokens, long cachedTokens) {
}