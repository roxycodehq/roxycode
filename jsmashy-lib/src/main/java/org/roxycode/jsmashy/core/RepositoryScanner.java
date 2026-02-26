package org.roxycode.jsmashy.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import org.roxycode.jsmashy.languages.LanguageAnalyzer;
import org.roxycode.jsmashy.languages.JavaLanguageAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryScanner {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryScanner.class);
    private final List<LanguageAnalyzer> analyzers;

    public RepositoryScanner(List<LanguageAnalyzer> analyzers) {
        this.analyzers = analyzers;
    }

    public RepositoryScanner() {
        this(List.of(new JavaLanguageAnalyzer()));
    }

    public List<ProjectFile> scan(Path rootDir) throws IOException {
        logger.info("Starting repository scan: {}", rootDir);
        Path absoluteRoot = rootDir.toAbsolutePath().normalize();
        List<ProjectFile> files = new ArrayList<>();
        Files.walkFileTree(absoluteRoot, new ProjectFileVisitor(absoluteRoot, files, analyzers));
        logger.info("Scan complete. Found {} files.", files.size());
        return files;
    }
}
