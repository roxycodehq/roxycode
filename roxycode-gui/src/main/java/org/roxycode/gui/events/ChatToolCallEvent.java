package org.roxycode.gui.events;

import java.util.Map;

public record ChatToolCallEvent(String toolName, Map<String, Object> arguments) {
}