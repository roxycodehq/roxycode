package org.roxycode.jsmashy.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectFileVisitorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testVisitFiles() throws IOException {
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.writeString(tempDir.resolve("file1.txt"), "content1");
        
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Files.createFile(subDir.resolve("file2.txt"));
        Files.writeString(subDir.resolve("file2.txt"), "content2");

        List<ProjectFile> files = new ArrayList<>();
        ProjectFileVisitor visitor = new ProjectFileVisitor(tempDir, files);
        Files.walkFileTree(tempDir, visitor);

        assertEquals(2, files.size());
        
        boolean found1 = files.stream().anyMatch(f -> f.getPath().equals("file1.txt") && f.getContent().equals("content1"));
        boolean found2 = files.stream().anyMatch(f -> f.getPath().contains("file2.txt") && f.getContent().equals("content2"));
        
        assertTrue(found1, "file1.txt not found correctly");
        assertTrue(found2, "file2.txt not found correctly");
    }

    @Test
    public void testExclusions() throws IOException {
        Files.createDirectory(tempDir.resolve(".git"));
        Files.createFile(tempDir.resolve(".git/config"));
        
        Files.createFile(tempDir.resolve("allowed.txt"));

        List<ProjectFile> files = new ArrayList<>();
        ProjectFileVisitor visitor = new ProjectFileVisitor(tempDir, files);
        Files.walkFileTree(tempDir, visitor);

        assertEquals(1, files.size());
        assertEquals("allowed.txt", files.get(0).getPath());
    }
}
