package org.roxycode.jsmashy.cli;

import org.roxycode.jsmashy.core.ProjectFile;
import org.roxycode.jsmashy.core.RepositoryScanner;
import org.roxycode.jsmashy.formatters.XmlSmashFormatter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: jsmashy <input-dir> <output-file>");
            System.exit(1);
        }

        Path inputDir = Paths.get(args[0]);
        Path outputFile = Paths.get(args[1]);

        try {
            System.out.println("Scanning directory: " + inputDir);
            RepositoryScanner scanner = new RepositoryScanner();
            List<ProjectFile> files = scanner.scan(inputDir);

            System.out.println("Formatting output...");
            XmlSmashFormatter formatter = new XmlSmashFormatter();
            String output = formatter.format(files);

            System.out.println("Writing to: " + outputFile);
            Files.writeString(outputFile, output);

            System.out.println("Done! Processed " + files.size() + " files.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
