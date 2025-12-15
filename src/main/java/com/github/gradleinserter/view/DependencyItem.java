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

    // Patterns for parsing dependency coordinates
    // Updated to handle versions with property references like ${version} or $version
    private static final Pattern COORDINATE_PATTERN =
            Pattern.compile("([^:]+):([^:]+)(?::([^:@]+?))?(?:@(.+))?");

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
        Matcher matcher = COORDINATE_PATTERN.matcher(notation);
        if (matcher.matches()) {
            return new DependencyItem(
                    config,
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4),
                    notation,
                    node,
                    excludes
            );
        }

        // Fallback for unparseable notation
        return new DependencyItem(config, "", notation, null, null, notation, node, excludes);
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
