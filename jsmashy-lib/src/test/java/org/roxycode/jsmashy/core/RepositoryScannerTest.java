package org.roxycode.jsmashy.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class RepositoryScannerTest {

    @TempDir
    Path tempDir;

    @Test
    public void testDefaultScannerSkeletonizesJava() throws IOException {
        String javaCode = "public class App { public void run() { System.out.println(\"hi\"); } }";
        Files.writeString(tempDir.resolve("App.java"), javaCode);
        
        RepositoryScanner scanner = new RepositoryScanner();
        List<ProjectFile> files = scanner.scan(tempDir);
        
        assertEquals(1, files.size());
        ProjectFile file = files.get(0);
        assertEquals("App.java", file.getPath());
        assertTrue(file.getContent().contains("/* implementation omitted */"), "Java file should be skeletonized by default");
    }

    @Test
    public void testCustomAnalyzers() throws IOException {
        String javaCode = "public class App {}";
        Files.writeString(tempDir.resolve("App.java"), javaCode);
        
        // Pass empty list - no skeletonization
        RepositoryScanner scanner = new RepositoryScanner(java.util.Collections.emptyList());
        List<ProjectFile> files = scanner.scan(tempDir);
        
        assertEquals(1, files.size());
        assertFalse(files.get(0).getContent().contains("/* implementation omitted */"));
    }
}
