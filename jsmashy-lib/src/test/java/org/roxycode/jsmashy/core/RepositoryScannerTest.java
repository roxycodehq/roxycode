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

    @Test
    public void testSkipBinaryFiles() throws IOException {
        // Create a binary file with invalid UTF-8 (0xFF is not a valid start byte in UTF-8)
        byte[] binaryData = new byte[] { (byte)0xFF, (byte)0xFE, (byte)0xFD };
        Files.write(tempDir.resolve("binary.dat"), binaryData);
        
        RepositoryScanner scanner = new RepositoryScanner();
        // This should not throw an exception after the fix
        List<ProjectFile> files = scanner.scan(tempDir);
        
        // Ensure the binary file was skipped but the scan finished
        boolean foundBinary = files.stream().anyMatch(f -> f.getPath().equals("binary.dat"));
        assertFalse(foundBinary, "Binary file should be skipped");
    }

}