package org.roxycode.jsmashy.formatters;

import org.roxycode.jsmashy.core.ProjectFile;
import org.roxycode.jsmashy.core.SmashFormatter;

import java.util.List;

public class XmlSmashFormatter implements SmashFormatter {
    @Override
    public String format(List<ProjectFile> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<project>\n");
        for (ProjectFile file : files) {
            sb.append("  <file path=\"").append(file.getPath()).append("\">\n");
            sb.append("    <content><![CDATA[");
            sb.append(file.getContent());
            sb.append("]]></content>\n");
            sb.append("  </file>\n");
        }
        sb.append("</project>");
        return sb.toString();
    }
}