package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.IRNode;
import com.github.gradleinserter.ir.MethodCallNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a single dependency declaration.
 * Example: implementation 'com.google.guava:guava:31.0'
 */
public final class DependencyItem {

    // For parsing dependency coordinates - we'll use manual parsing for complex versions

    @NotNull
    private final String configuration;  // implementation, api, testImplementation, etc.
    @NotNull
    private final String group;
    @NotNull
    private final String name;
    @Nullable
    private final String version;        // may be null
    @Nullable
    private final String classifier;     // may be null
    @NotNull
    private final String rawNotation;    // original notation string
    @Nullable
    private final MethodCallNode sourceNode;
    @NotNull
    private final List<ExcludeItem> excludes;

    private DependencyItem(@NotNull String configuration, @NotNull String group, @NotNull String name,
                           @Nullable String version, @Nullable String classifier, @NotNull String rawNotation,
                           @Nullable MethodCallNode sourceNode, @NotNull List<ExcludeItem> excludes) {
        this.configuration = configuration;
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
        this.rawNotation = rawNotation;
        this.sourceNode = sourceNode;
        this.excludes = Collections.unmodifiableList(new ArrayList<>(excludes));
    }

    @NotNull
    public static DependencyItem fromMethodCall(@NotNull MethodCallNode node) {
        String config = node.getMethodName();
        String arg = node.getFirstArgument();

        // Parse excludes from closure body if present
        List<ExcludeItem> excludes = parseExcludes(node);

        // Handle map-style notation: group: 'x', name: 'y', version: 'z'
        if (arg.contains(":") && arg.contains(",")) {
            return parseMapNotation(config, arg, node, excludes);
        }

        // Handle string notation: 'group:name:version'
        return parseStringNotation(config, arg, node, excludes);
    }

    @NotNull
    private static List<ExcludeItem> parseExcludes(@NotNull MethodCallNode node) {
        List<ExcludeItem> excludes = new ArrayList<>();
        if (!node.hasClosureBody()) {
            return excludes;
        }

        // Get the source text of the closure body and look for exclude statements
        IRNode closureBody = node.getClosureBody();
        if (closureBody == null) {
            return excludes;
        }

        String source = closureBody.getSourceText();
        if (source == null || source.isEmpty()) {
            return excludes;
        }

        // Pattern to find exclude statements
        Pattern excludePattern = Pattern.compile("exclude\\s+[^\\n]+", Pattern.MULTILINE);
        Matcher matcher = excludePattern.matcher(source);
        while (matcher.find()) {
            excludes.add(ExcludeItem.parse(matcher.group()));
        }

        return excludes;
    }

    @NotNull
    private static DependencyItem parseStringNotation(@NotNull String config, @NotNull String notation,
                                                      @NotNull MethodCallNode node,
                                                      @NotNull List<ExcludeItem> excludes) {
        // Parse coordinates manually to handle complex versions like ${version}, $var, etc.
        ParsedCoordinates coords = parseCoordinates(notation);

        return new DependencyItem(
                config,
                coords.group,
                coords.name,
                coords.version,
                coords.classifier,
                notation,
                node,
                excludes
        );
    }

    /**
     * Parse dependency coordinates from string notation.
     * Handles complex versions like ${version}, $var, ${props['key']}, etc.
     */
    @NotNull
    private static ParsedCoordinates parseCoordinates(@NotNull String notation) {
        String group = "";
        String name = "";
        String version = null;
        String classifier = null;

        // First, handle classifier (@extension)
        int atIndex = notation.lastIndexOf('@');
        String mainPart = notation;
        if (atIndex >= 0) {
            classifier = notation.substring(atIndex + 1);
            mainPart = notation.substring(0, atIndex);
        }

        // Split by colons, but be careful with property references
        // We need to find exactly 2 or 3 parts: group:name or group:name:version
        int firstColon = mainPart.indexOf(':');
        if (firstColon < 0) {
            // No colon - single value (probably just name)
            name = mainPart;
            return new ParsedCoordinates(group, name, version, classifier);
        }

        group = mainPart.substring(0, firstColon);
        String afterFirstColon = mainPart.substring(firstColon + 1);

        // Find the second colon for version separation
        int secondColon = findVersionSeparator(afterFirstColon);

        if (secondColon < 0) {
            // No version - just group:name
            name = afterFirstColon;
        } else {
            name = afterFirstColon.substring(0, secondColon);
            version = afterFirstColon.substring(secondColon + 1);
        }

        return new ParsedCoordinates(group, name, version, classifier);
    }

    /**
     * Find the colon that separates artifact name from version.
     * Must handle complex versions that may contain colons inside expressions.
     */
    private static int findVersionSeparator(@NotNull String afterGroup) {
        int braceDepth = 0;
        int bracketDepth = 0;
        int parenDepth = 0;

        for (int i = 0; i < afterGroup.length(); i++) {
            char c = afterGroup.charAt(i);

            switch (c) {
                case '{':
                    braceDepth++;
                    break;
                case '}':
                    braceDepth--;
                    break;
                case '[':
                    bracketDepth++;
                    break;
                case ']':
                    bracketDepth--;
                    break;
                case '(':
                    parenDepth++;
                    break;
                case ')':
                    parenDepth--;
                    break;
                case ':':
                    // Only treat as separator if we're not inside any brackets/braces/parens
                    if (braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                        return i;
                    }
                    break;
            }
        }

        return -1;
    }

    /**
     * Helper class for parsed coordinates.
     */
    private static class ParsedCoordinates {
        @NotNull final String group;
        @NotNull final String name;
        @Nullable final String version;
        @Nullable final String classifier;

        ParsedCoordinates(@NotNull String group, @NotNull String name,
                          @Nullable String version, @Nullable String classifier) {
            this.group = group;
            this.name = name;
            this.version = version;
            this.classifier = classifier;
        }
    }

    @NotNull
    private static DependencyItem parseMapNotation(@NotNull String config, @NotNull String notation,
                                                   @NotNull MethodCallNode node,
                                                   @NotNull List<ExcludeItem> excludes) {
        String group = extractMapValue(notation, "group");
        String name = extractMapValue(notation, "name");
        String version = extractMapValue(notation, "version");

        return new DependencyItem(config, group, name, version, null, notation, node, excludes);
    }

    @NotNull
    private static String extractMapValue(@NotNull String notation, @NotNull String key) {
        Pattern pattern = Pattern.compile(key + "\\s*:\\s*['\"]?([^'\"\\s,]+)['\"]?");
        Matcher matcher = pattern.matcher(notation);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    @NotNull
    public String getConfiguration() {
        return configuration;
    }

    @NotNull
    public String getGroup() {
        return group;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getVersion() {
        return version;
    }

    @Nullable
    public String getClassifier() {
        return classifier;
    }

    @NotNull
    public String getRawNotation() {
        return rawNotation;
    }

    @Nullable
    public MethodCallNode getSourceNode() {
        return sourceNode;
    }

    @NotNull
    public List<ExcludeItem> getExcludes() {
        return excludes;
    }

    /**
     * @return true if this dependency has any exclude declarations
     */
    public boolean hasExcludes() {
        return !excludes.isEmpty();
    }

    /**
     * @return group:name (without version) for matching purposes
     */
    @NotNull
    public String getCoordinateKey() {
        return group + ":" + name;
    }

    /**
     * @return full coordinate with version
     */
    @NotNull
    public String getFullCoordinate() {
        if (version != null && !version.isEmpty()) {
            return group + ":" + name + ":" + version;
        }
        return getCoordinateKey();
    }

    /**
     * Get the original source text for this dependency, preserving the exact notation style.
     * Useful when adding a dependency from a snippet - preserves map notation, quotes, etc.
     *
     * @return the original source text, or a reconstructed string if source is not available
     */
    @NotNull
    public String getOriginalSourceText() {
        if (sourceNode != null) {
            String sourceText = sourceNode.getSourceText();
            if (sourceText != null && !sourceText.isEmpty()) {
                return sourceText;
            }
        }
        // Fallback to reconstructed string
        return configuration + " '" + getFullCoordinate() + "'";
    }

    /**
     * Create a new DependencyItem with updated version.
     */
    @NotNull
    public DependencyItem withVersion(@Nullable String newVersion) {
        return new DependencyItem(configuration, group, name, newVersion, classifier,
                rawNotation, sourceNode, excludes);
    }

    /**
     * @return true if this represents the same dependency (ignoring version)
     */
    public boolean sameArtifact(@NotNull DependencyItem other) {
        return Objects.equals(group, other.group) && Objects.equals(name, other.name);
    }

    /**
     * @return true if this represents the same dependency with the same configuration (ignoring version)
     */
    public boolean sameConfigurationAndArtifact(@NotNull DependencyItem other) {
        return Objects.equals(configuration, other.configuration)
                && Objects.equals(group, other.group)
                && Objects.equals(name, other.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DependencyItem)) return false;
        DependencyItem that = (DependencyItem) o;
        return Objects.equals(configuration, that.configuration)
                && Objects.equals(group, that.group)
                && Objects.equals(name, that.name)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration, group, name, version);
    }

    @Override
    public String toString() {
        return configuration + " '" + getFullCoordinate() + "'";
    }
}
