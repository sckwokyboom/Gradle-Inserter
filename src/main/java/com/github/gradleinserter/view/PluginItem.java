package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.MethodCallNode;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a single plugin declaration.
 * Example: id 'java-library' version '1.0'
 */
public final class PluginItem {

    private static final Pattern VERSION_PATTERN = Pattern.compile("version\\s*['\"]([^'\"]+)['\"]");

    private final String id;
    private final String version;  // may be null
    private final boolean apply;   // false if 'apply false'
    private final MethodCallNode sourceNode;

    public PluginItem(String id, String version, boolean apply, MethodCallNode sourceNode) {
        this.id = Objects.requireNonNull(id, "id");
        this.version = version;
        this.apply = apply;
        this.sourceNode = sourceNode;
    }

    public static PluginItem fromMethodCall(MethodCallNode node) {
        String id = "";
        String version = null;
        boolean apply = true;

        String methodName = node.getMethodName();
        String arg = node.getFirstArgument();

        if ("id".equals(methodName)) {
            id = arg;
            // Check for version in source
            String source = node.getSourceText();
            Matcher matcher = VERSION_PATTERN.matcher(source);
            if (matcher.find()) {
                version = matcher.group(1);
            }
            if (source.contains("apply false")) {
                apply = false;
            }
        } else if ("kotlin".equals(methodName) || "java".equals(methodName)) {
            id = "org.jetbrains.kotlin." + arg;
        } else {
            // Shorthand notation like: java, application, etc.
            id = methodName;
        }

        return new PluginItem(id, version, apply, node);
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

    /**
     * Create a new PluginItem with updated version.
     */
    public PluginItem withVersion(String newVersion) {
        return new PluginItem(id, newVersion, apply, sourceNode);
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
