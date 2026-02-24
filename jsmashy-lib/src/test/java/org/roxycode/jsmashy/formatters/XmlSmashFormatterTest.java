package org.roxycode.jsmashy.formatters;

import org.junit.jupiter.api.Test;
import org.roxycode.jsmashy.core.ProjectFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlSmashFormatterTest {
    @Test
    public void testFormat() {
        XmlSmashFormatter formatter = new XmlSmashFormatter();
        List<ProjectFile> files = List.of(
            new ProjectFile("test.txt", "Hello World")
        );
        String result = formatter.format(files);
        
        assertTrue(result.contains("<total_files>1</total_files>"));
        assertTrue(result.contains("test.txt"));
        assertTrue(result.contains("Hello World"));
    }
}
