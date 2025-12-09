package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.MethodCallNode;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a single dependency declaration.
 * Example: implementation 'com.google.guava:guava:31.0'
 */
public final class DependencyItem {

    // Patterns for parsing dependency coordinates
    private static final Pattern COORDINATE_PATTERN =
            Pattern.compile("([^:]+):([^:]+)(?::([^:@]+))?(?:@(.+))?");

    private final String configuration;  // implementation, api, testImplementation, etc.
    private final String group;
    private final String name;
    private final String version;        // may be null
    private final String classifier;     // may be null
    private final String rawNotation;    // original notation string
    private final MethodCallNode sourceNode;

    private DependencyItem(String configuration, String group, String name,
                           String version, String classifier, String rawNotation,
                           MethodCallNode sourceNode) {
        this.configuration = configuration;
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
        this.rawNotation = rawNotation;
        this.sourceNode = sourceNode;
    }

    public static DependencyItem fromMethodCall(MethodCallNode node) {
        String config = node.getMethodName();
        String arg = node.getFirstArgument();

        // Handle map-style notation: group: 'x', name: 'y', version: 'z'
        if (arg.contains(":") && arg.contains(",")) {
            return parseMapNotation(config, arg, node);
        }

        // Handle string notation: 'group:name:version'
        return parseStringNotation(config, arg, node);
    }

    private static DependencyItem parseStringNotation(String config, String notation,
                                                      MethodCallNode node) {
        Matcher matcher = COORDINATE_PATTERN.matcher(notation);
        if (matcher.matches()) {
            return new DependencyItem(
                    config,
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4),
                    notation,
                    node
            );
        }

        // Fallback for unparseable notation
        return new DependencyItem(config, "", notation, null, null, notation, node);
    }

    private static DependencyItem parseMapNotation(String config, String notation,
                                                   MethodCallNode node) {
        String group = extractMapValue(notation, "group");
        String name = extractMapValue(notation, "name");
        String version = extractMapValue(notation, "version");

        return new DependencyItem(config, group, name, version, null, notation, node);
    }

    private static String extractMapValue(String notation, String key) {
        Pattern pattern = Pattern.compile(key + "\\s*:\\s*['\"]?([^'\"\\s,]+)['\"]?");
        Matcher matcher = pattern.matcher(notation);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public String getConfiguration() {
        return configuration;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getRawNotation() {
        return rawNotation;
    }

    public MethodCallNode getSourceNode() {
        return sourceNode;
    }

    /**
     * @return group:name (without version) for matching purposes
     */
    public String getCoordinateKey() {
        return group + ":" + name;
    }

    /**
     * @return full coordinate with version
     */
    public String getFullCoordinate() {
        if (version != null && !version.isEmpty()) {
            return group + ":" + name + ":" + version;
        }
        return getCoordinateKey();
    }

    /**
     * Create a new DependencyItem with updated version.
     */
    public DependencyItem withVersion(String newVersion) {
        return new DependencyItem(configuration, group, name, newVersion, classifier,
                rawNotation, sourceNode);
    }

    /**
     * @return true if this represents the same dependency (ignoring version)
     */
    public boolean sameArtifact(DependencyItem other) {
        return Objects.equals(group, other.group) && Objects.equals(name, other.name);
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
