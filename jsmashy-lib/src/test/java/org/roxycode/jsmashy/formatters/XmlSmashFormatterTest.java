package org.roxycode.jsmashy.formatters;

import org.junit.jupiter.api.Test;
import org.roxycode.jsmashy.core.ProjectFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlSmashFormatterTest {
    @Test
    void testFormat() {
        XmlSmashFormatter formatter = new XmlSmashFormatter();
        List<ProjectFile> files = List.of(
            new ProjectFile("src/Main.java", "public class Main {}"),
            new ProjectFile("README.md", "# Hello"),
            new ProjectFile("AGENTS.md", "Agent instructions here")
        );

        String output = formatter.format(files);

        // Check for summary section
        assertTrue(output.contains("<summary>"));
        assertTrue(output.contains("<agent_instructions>"));
        assertTrue(output.contains("Agent instructions here"));

        // Check integrated tree structure
        assertTrue(output.contains("<project_tree>"));
        assertTrue(output.contains("<d n=\"src\">"));
        assertTrue(output.contains("<f n=\"Main.java\">"));
        assertTrue(output.contains("public class Main {}"));
        assertTrue(output.contains("<f n=\"README.md\">"));
        assertTrue(output.contains("# Hello"));
    }
}
