package org.roxycode.jsmashy.languages;

import java.util.List;

/**
 * Represents the results of a source code analysis.
 */
public record AnalysisResult(
    String skeleton,
    List<String> errors
) {
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
