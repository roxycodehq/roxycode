package org.roxycode.jsmashy.core;

import org.eclipse.jgit.ignore.IgnoreNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * File visitor that collects files into a list of ProjectFile objects.
 * Respects .gitignore rules using JGit.
 */
public class ProjectFileVisitor extends SimpleFileVisitor<Path> {
    private final Path rootDir;
    private final List<ProjectFile> files;
    
    private static class IgnoreEntry {
        final Path dir;
        final IgnoreNode node;
        IgnoreEntry(Path dir, IgnoreNode node) { this.dir = dir; this.node = node; }
    }
    
    private final Deque<IgnoreEntry> ignoreStack = new ArrayDeque<>();

    public ProjectFileVisitor(Path rootDir, List<ProjectFile> files) {
        this.rootDir = rootDir;
        this.files = files;
    }

    private boolean isIgnored(Path path, boolean isDir) {
        String name = path.getFileName() != null ? path.getFileName().toString() : "";
        // Hardcoded defaults
        if (name.equals(".git")) return true;

        for (IgnoreEntry entry : ignoreStack) {
            try {
                Path relativePath = entry.dir.relativize(path);
                String pathString = relativePath.toString().replace('\\', '/');
                if (pathString.isEmpty()) continue;
                
                if (isDir && !pathString.endsWith("/")) {
                    pathString += "/";
                }
                
                IgnoreNode.MatchResult result = entry.node.isIgnored(pathString, isDir);
                if (result == IgnoreNode.MatchResult.IGNORED) return true;
                if (result == IgnoreNode.MatchResult.NOT_IGNORED) return false;
            } catch (IllegalArgumentException e) {
                // Path is not relative to this entry.dir, skip
            }
        }
        return false;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (isIgnored(dir, true)) {
            return FileVisitResult.SKIP_SUBTREE;
        }

        Path gitignore = dir.resolve(".gitignore");
        if (Files.exists(gitignore)) {
            IgnoreNode node = new IgnoreNode();
            try (InputStream is = Files.newInputStream(gitignore)) {
                node.parse(is);
            }
            ignoreStack.push(new IgnoreEntry(dir, node));
        }
        
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        if (!ignoreStack.isEmpty() && ignoreStack.peek().dir.equals(dir)) {
            ignoreStack.pop();
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (isIgnored(file, false)) {
            return FileVisitResult.CONTINUE;
        }
        
        String relativePath = rootDir.relativize(file).toString();
        String content = Files.readString(file);
        files.add(new ProjectFile(relativePath, content));
        
        return FileVisitResult.CONTINUE;
    }
}
