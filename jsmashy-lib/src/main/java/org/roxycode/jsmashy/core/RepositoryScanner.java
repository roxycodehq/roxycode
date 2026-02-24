package org.roxycode.jsmashy.core;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class RepositoryScanner {
    public List<ProjectFile> scan(Path rootDir) throws IOException {
        List<ProjectFile> files = new ArrayList<>();
        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isRegularFile(file)) {
                    String relativePath = rootDir.relativize(file).toString();
                    String content = Files.readString(file);
                    files.add(new ProjectFile(relativePath, content));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String name = dir.getFileName().toString();
                if (name.equals(".git") || name.equals("target")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }
}
