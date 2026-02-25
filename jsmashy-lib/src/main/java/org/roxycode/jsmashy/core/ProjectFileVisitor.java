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
import org.roxycode.jsmashy.languages.LanguageAnalyzer;
import org.roxycode.jsmashy.languages.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * File visitor that collects files into a list of ProjectFile objects.
 * Respects .gitignore rules using JGit.
 */
public class ProjectFileVisitor extends SimpleFileVisitor<Path> {
    private static final Logger logger = LoggerFactory.getLogger(ProjectFileVisitor.class);

    private final Path rootDir;
    private final List<ProjectFile> files;
    private final List<LanguageAnalyzer> analyzers;
    
    private static class IgnoreEntry {
        final Path dir;
        final IgnoreNode node;
        IgnoreEntry(Path dir, IgnoreNode node) { this.dir = dir; this.node = node; }
    }
    
    private final Deque<IgnoreEntry> ignoreStack = new ArrayDeque<>();

    public ProjectFileVisitor(Path rootDir, List<ProjectFile> files) {
        this(rootDir, files, new java.util.ArrayList<>());
    }

    public ProjectFileVisitor(Path rootDir, List<ProjectFile> files, List<LanguageAnalyzer> analyzers) {
        this.rootDir = rootDir;
        this.files = files;
        this.analyzers = analyzers;
    }

    private boolean isIgnored(Path path, boolean isDir) {
        String name = path.getFileName() != null ? path.getFileName().toString() : "";
        // Hardcoded defaults
        if (name.equals(".git") || name.equals(".jsmashyignore")) return true;

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

        private void loadIgnoreFile(Path dir, String fileName) throws IOException {
        Path ignoreFile = dir.resolve(fileName);
        if (Files.exists(ignoreFile)) {
            IgnoreNode node = new IgnoreNode();
            try (InputStream is = Files.newInputStream(ignoreFile)) {
                node.parse(is);
            }
            ignoreStack.push(new IgnoreEntry(dir, node));
        }
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (isIgnored(dir, true)) {
            logger.debug("Skipping ignored directory: {}", dir);
            return FileVisitResult.SKIP_SUBTREE;
        }

        loadIgnoreFile(dir, ".gitignore");
        loadIgnoreFile(dir, ".jsmashyignore");
        
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        while (!ignoreStack.isEmpty() && ignoreStack.peek().dir.equals(dir)) {
            ignoreStack.pop();
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (isIgnored(file, false)) {
            logger.debug("Skipping ignored file: {}", file);
            return FileVisitResult.CONTINUE;
        }
        
        String relativePath = rootDir.relativize(file).toString();
        String content = Files.readString(file);
        String fileName = file.getFileName().toString();

        if (analyzers != null) {
            for (LanguageAnalyzer analyzer : analyzers) {
                if (analyzer.supports(fileName)) {
                    AnalysisResult result = analyzer.analyze(content);
                    if (!result.hasErrors()) {
                        content = result.skeleton();
                    }
                    break;
                }
            }
        }
        files.add(new ProjectFile(relativePath, content));
        logger.info("Processed: {}", relativePath);
        
        return FileVisitResult.CONTINUE;
    }
}
