package org.roxycode.jsmashy.cli;

import org.roxycode.jsmashy.core.ProjectFile;
import org.roxycode.jsmashy.core.RepositoryScanner;
import org.roxycode.jsmashy.formatters.XmlSmashFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            logger.error("Usage: jsmashy <input-dir> <output-file>");
            System.exit(1);
        }

        Path inputDir = Paths.get(args[0]);
        Path outputFile = Paths.get(args[1]);

        if (!Files.exists(inputDir)) {
            logger.error("Input directory does not exist: {}", inputDir);
            System.exit(1);
        }

        try {
            logger.info("Scanning directory: {}", inputDir);
            RepositoryScanner scanner = new RepositoryScanner();
            List<ProjectFile> files = scanner.scan(inputDir);

            logger.info("Formatting output...");
            XmlSmashFormatter formatter = new XmlSmashFormatter();
            String xml = formatter.format(files);

            logger.info("Writing to: {}", outputFile);
            Files.writeString(outputFile, xml);

            logger.info("Done! Processed {} files.", files.size());
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
