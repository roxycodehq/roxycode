package org.roxycode.jsmashy.formatters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.roxycode.jsmashy.core.ProjectFile;
import org.roxycode.jsmashy.core.SmashFormatter;

public class XmlSmashFormatter implements SmashFormatter {

    @Override
    public String format(List<ProjectFile> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<codebase>\n");

        appendFileSummary(sb, files);
        appendProjectTree(sb, files);

        sb.append("</codebase>");
        return sb.toString();
    }

    private void appendFileSummary(StringBuilder sb, List<ProjectFile> files) {
        sb.append("<summary>\n");
        sb.append("<agent_instructions>\n");
        sb.append("The <project_tree> section shows the directory hierarchy.\n");
        sb.append("<d n=\"name\"> is a directory, <f n=\"name\"> is a file containing CDATA content.\n");
        sb.append("</agent_instructions>\n");

        // AGENTS.md extraction
        for (ProjectFile file : files) {
            if (file.getPath().equalsIgnoreCase("AGENTS.md")) {
                sb.append("<agent_custom_instructions>\n<![CDATA[\n");
                sb.append(file.getContent().replace("]]>", "]]]]><![CDATA[>"));
                sb.append("\n]]>\n</agent_custom_instructions>\n");
                break;
            }
        }

        sb.append("</summary>\n\n");
    }

    private void appendProjectTree(StringBuilder sb, List<ProjectFile> files) {
        TreeNode root = new TreeNode("");
        for (ProjectFile file : files) {
            String[] parts = file.getPath().replace('\\', '/').split("/");
            TreeNode current = root;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.isEmpty()) {
                    continue;
                }
                current = current.children.computeIfAbsent(part, k -> new TreeNode(k));
                if (i == parts.length - 1) {
                    current.file = file;
                }
            }
        }

        sb.append("<project_tree>\n");
        renderXmlTree(root, "", sb);
        sb.append("</project_tree>\n");
    }

    private void renderXmlTree(TreeNode node, String indent, StringBuilder sb) {
        List<String> sortedKeys = new ArrayList<>(node.children.keySet());
        Collections.sort(sortedKeys);

        for (String key : sortedKeys) {
            TreeNode child = node.children.get(key);
            if (child.file != null) {
                // It's a file
                sb.append(indent).append("<f n=\"").append(key).append("\">");
                sb.append("<![CDATA[");
                sb.append(child.file.getContent().replace("]]>", "]]]]><![CDATA[>"));
                sb.append("]]>");
                sb.append("</f>\n");
            } else {
                // It's a directory
                sb.append(indent).append("<d n=\"").append(key).append("\">\n");
                renderXmlTree(child, indent + "  ", sb);
                sb.append(indent).append("</d>\n");
            }
        }
    }

    private static class TreeNode {

        String name;
        ProjectFile file;
        Map<String, TreeNode> children = new HashMap<>();

        TreeNode(String name) {
            this.name = name;
        }
    }
}
