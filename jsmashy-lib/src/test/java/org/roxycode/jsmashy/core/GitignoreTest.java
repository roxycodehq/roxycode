package org.roxycode.jsmashy.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GitignoreTest {
    @TempDir
    Path tempDir;

    @Test
    void testGitignoreFiltering() throws IOException {
        // Setup
        Files.writeString(tempDir.resolve(".gitignore"), "*.log\ntemp/\n");
        Files.writeString(tempDir.resolve("keep.txt"), "keep");
        Files.writeString(tempDir.resolve("ignore.log"), "ignore");
        
        Path subDir = tempDir.resolve("temp");
        Files.createDirectory(subDir);
        Files.writeString(subDir.resolve("hidden.txt"), "hidden");
        
        Path nestedDir = tempDir.resolve("nested");
        Files.createDirectory(nestedDir);
        Files.writeString(nestedDir.resolve("nested.txt"), "nested");
        Files.writeString(nestedDir.resolve(".gitignore"), "!nested.log\n");
        Files.writeString(nestedDir.resolve("nested.log"), "should keep due to negation");

        RepositoryScanner scanner = new RepositoryScanner();
        List<ProjectFile> files = scanner.scan(tempDir);

        boolean foundKeep = false;
        boolean foundIgnoreLog = false;
        boolean foundHidden = false;
        boolean foundNested = false;
        boolean foundNestedLog = false;

        for (ProjectFile pf : files) {
            String path = pf.getPath().replace('\\', '/');
            if (path.equals("keep.txt")) foundKeep = true;
            if (path.equals("ignore.log")) foundIgnoreLog = true;
            if (path.equals("temp/hidden.txt")) foundHidden = true;
            if (path.equals("nested/nested.txt")) foundNested = true;
            if (path.equals("nested/nested.log")) foundNestedLog = true;
        }

        assertTrue(foundKeep, "keep.txt should be found");
        assertFalse(foundIgnoreLog, "ignore.log should be filtered");
        assertFalse(foundHidden, "temp/hidden.txt should be filtered");
        assertTrue(foundNested, "nested/nested.txt should be found");
        assertTrue(foundNestedLog, "nested/nested.log should be found due to negation in nested .gitignore");
    }

    @Test
    void testParentGitignoreFiltering() throws IOException {
        // Setup: parentDir/.gitignore and parentDir/subDir (which we will scan)
        Path parentDir = tempDir.resolve("parent");
        Files.createDirectory(parentDir);
        Files.writeString(parentDir.resolve(".gitignore"), "*.log\n");
        Files.writeString(parentDir.resolve(".git"), ""); // Mock .git file/dir to stop traversal

        Path subDir = parentDir.resolve("sub");
        Files.createDirectory(subDir);
        Files.writeString(subDir.resolve("keep.txt"), "keep");
        Files.writeString(subDir.resolve("ignore.log"), "ignore");

        RepositoryScanner scanner = new RepositoryScanner();
        // Scanning 'subDir' but it should pick up rules from 'parentDir'
        List<ProjectFile> files = scanner.scan(subDir);

        boolean foundKeep = false;
        boolean foundIgnoreLog = false;

        for (ProjectFile pf : files) {
            String path = pf.getPath().replace('\\', '/');
            if (path.equals("keep.txt")) foundKeep = true;
            if (path.equals("ignore.log")) foundIgnoreLog = true;
        }

        assertTrue(foundKeep, "keep.txt should be found");
        assertFalse(foundIgnoreLog, "ignore.log should be filtered by parent .gitignore");
    }
}