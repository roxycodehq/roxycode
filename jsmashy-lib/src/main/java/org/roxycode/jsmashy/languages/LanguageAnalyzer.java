package org.roxycode.jsmashy.languages;

/**
 * Interface for language-specific source code analysis and skeletonization.
 */
public interface LanguageAnalyzer {
    /**
     * Checks if this analyzer supports the given file.
     * @param fileName The name of the file.
     * @return true if supported, false otherwise.
     */
    boolean supports(String fileName);

    /**
     * Analyzes the source code and returns a skeletonized version.
     * @param sourceCode The source code to analyze.
     * @return The analysis result.
     */
    AnalysisResult analyze(String sourceCode);
}