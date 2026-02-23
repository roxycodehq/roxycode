package org.roxycode.jsmashy.core;

public class ProjectFile {
    private final String path;
    private final String content;

    public ProjectFile(String path, String content) {
        this.path = path;
        this.content = content;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }
}