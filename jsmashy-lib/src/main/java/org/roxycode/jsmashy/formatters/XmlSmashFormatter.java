package org.roxycode.jsmashy.formatters;

import org.roxycode.jsmashy.core.ProjectFile;
import org.roxycode.jsmashy.core.SmashFormatter;

import java.util.List;
import java.util.stream.Collectors;

public class XmlSmashFormatter implements SmashFormatter {
    @Override
    public String format(List<ProjectFile> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<codebase>\n");
        
        // Summary section
        sb.append("  <summary>\n");
        sb.append("    <total_files>").append(files.size()).append("</total_files>\n");
        sb.append("  </summary>\n");

        // Structure section (Simplified tree)
        sb.append("  <repository_structure>\n<![CDATA[\n");
        sb.append(generateTree(files));
        sb.append("\n]]>\n  </repository_structure>\n");

        // Files section
        for (ProjectFile file : files) {
            sb.append("  <file path=\"").append(file.getPath()).append("\">\n");
            sb.append("    <content><![CDATA[");
            // Basic CDATA escape logic if needed, but standard CDATA handles most
            sb.append(file.getContent().replace("]]>", "]]]]><![CDATA[>"));
            sb.append("]]></content>\n");
            sb.append("  </file>\n");
        }
        
        sb.append("</codebase>");
        return sb.toString();
    }

    private String generateTree(List<ProjectFile> files) {
        return files.stream()
                .map(ProjectFile::getPath)
                .sorted()
                .collect(Collectors.joining("\n"));
    }
}
