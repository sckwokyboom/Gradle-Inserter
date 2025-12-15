package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.MethodCallNode;
import com.github.gradleinserter.parser.ASTArgumentParser;

import java.util.Objects;

/**
 * Represents a single plugin declaration.
 * Example: id 'java-library' version '1.0'
 */
public final class PluginItem {

    private final String id;
    private final String version;  // may be null
    private final boolean apply;   // false if 'apply false'
    private final MethodCallNode sourceNode;
    private final String quoteStyle;  // ' or " - for preserving notation

    public PluginItem(String id, String version, boolean apply, MethodCallNode sourceNode) {
        this(id, version, apply, sourceNode, "'");
    }

    public PluginItem(String id, String version, boolean apply, MethodCallNode sourceNode, String quoteStyle) {
        this.id = Objects.requireNonNull(id, "id");
        this.version = version;
        this.apply = apply;
        this.sourceNode = sourceNode;
        this.quoteStyle = quoteStyle != null ? quoteStyle : "'";
    }

    public static PluginItem fromMethodCall(MethodCallNode node) {
        String id = "";
        String version = null;
        boolean apply = true;
        String quoteStyle = "'";

        String methodName = node.getMethodName();
        String arg = node.getFirstArgument();

        if ("id".equals(methodName)) {
            id = arg;
            // Check for version in source
            String source = node.getSourceText();

            // Detect quote style from source
            quoteStyle = detectQuoteStyle(source);

            // Use AST-based parser to extract version
            version = ASTArgumentParser.extractPluginVersion(source);

            if (source.contains("apply false")) {
                apply = false;
            }
        } else if ("kotlin".equals(methodName) || "java".equals(methodName)) {
            id = "org.jetbrains.kotlin." + arg;
        } else {
            // Shorthand notation like: java, application, etc.
            id = methodName;
        }

        return new PluginItem(id, version, apply, node, quoteStyle);
    }

    private static String detectQuoteStyle(String source) {
        // Find the first quote character in the source
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\'') {
                return "'";
            } else if (c == '"') {
                return "\"";
            }
        }
        return "'";  // default to single quote
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public boolean isApply() {
        return apply;
    }

    public MethodCallNode getSourceNode() {
        return sourceNode;
    }

    public String getQuoteStyle() {
        return quoteStyle;
    }

    /**
     * Create a new PluginItem with updated version.
     */
    public PluginItem withVersion(String newVersion) {
        return new PluginItem(id, newVersion, apply, sourceNode, quoteStyle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginItem)) return false;
        PluginItem that = (PluginItem) o;
        return apply == that.apply
                && Objects.equals(id, that.id)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, apply);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("id '").append(id).append("'");
        if (version != null) {
            sb.append(" version '").append(version).append("'");
        }
        if (!apply) {
            sb.append(" apply false");
        }
        return sb.toString();
    }
}
