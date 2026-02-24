package org.roxycode.jsmashy.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class RepositoryScanner {
    public List<ProjectFile> scan(Path rootDir) throws IOException {
        List<ProjectFile> files = new ArrayList<>();
        Files.walkFileTree(rootDir, new ProjectFileVisitor(rootDir, files));
        return files;
    }
}
