package org.roxycode.jsmashy.core;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * File visitor that collects files into a list of ProjectFile objects.
 * Handles directory exclusions and path relativization.
 */
public class ProjectFileVisitor extends SimpleFileVisitor<Path> {
    private final Path rootDir;
    private final List<ProjectFile> files;

    public ProjectFileVisitor(Path rootDir, List<ProjectFile> files) {
        this.rootDir = rootDir;
        this.files = files;
    }

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
}
