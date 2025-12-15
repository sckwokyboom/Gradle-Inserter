package com.github.gradleinserter.view;

import com.github.gradleinserter.parser.ASTArgumentParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an exclude declaration within a dependency.
 * Example: exclude group: 'com.example', module: 'some-module'
 */
public final class ExcludeItem {

    @Nullable
    private final String group;
    @Nullable
    private final String module;
    @NotNull
    private final String rawSource;

    public ExcludeItem(@Nullable String group, @Nullable String module, @NotNull String rawSource) {
        this.group = group;
        this.module = module;
        this.rawSource = rawSource;
    }

    /**
     * Parse an exclude item from source text using AST-based parsing.
     * Handles formats like:
     * - exclude group: 'com.example', module: 'some-module'
     * - exclude group: "com.example"
     * - exclude module: 'some-module'
     * - exclude group: ${groupVar}, module: ${moduleVar}
     * - exclude group: $groupVar
     */
    @NotNull
    public static ExcludeItem parse(@NotNull String source) {
        // Use AST-based parser to extract group and module
        String[] parsed = ASTArgumentParser.parseExcludeStatement(source);
        String group = parsed[0];
        String module = parsed[1];

        return new ExcludeItem(
                group != null && !group.isEmpty() ? group : null,
                module != null && !module.isEmpty() ? module : null,
                source.trim()
        );
    }

    @Nullable
    public String getGroup() {
        return group;
    }

    @Nullable
    public String getModule() {
        return module;
    }

    @NotNull
    public String getRawSource() {
        return rawSource;
    }

    /**
     * Check if this exclude matches another one by group and module.
     */
    public boolean matches(@NotNull ExcludeItem other) {
        return Objects.equals(group, other.group) && Objects.equals(module, other.module);
    }

    /**
     * Check if this exclude is broader than another (excludes more).
     * An exclude is broader if it has only group (excludes entire group) vs group+module.
     */
    public boolean isBroaderThan(@NotNull ExcludeItem other) {
        // If I exclude entire group (module==null) and other excludes specific module in same group
        if (this.module == null && other.module != null && Objects.equals(this.group, other.group)) {
            return true;
        }
        return false;
    }

    /**
     * Check if this exclude is narrower than another (excludes less).
     * An exclude is narrower if it has group+module vs only group.
     */
    public boolean isNarrowerThan(@NotNull ExcludeItem other) {
        return other.isBroaderThan(this);
    }

    /**
     * Check if this exclude covers another (i.e., makes it redundant).
     * This happens when:
     * - They match exactly, OR
     * - This exclude is broader (e.g., excludes whole group while other excludes specific module)
     */
    public boolean covers(@NotNull ExcludeItem other) {
        // Exact match
        if (matches(other)) {
            return true;
        }
        // This exclude is broader and covers the other
        if (isBroaderThan(other)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExcludeItem)) return false;
        ExcludeItem that = (ExcludeItem) o;
        return Objects.equals(group, that.group) && Objects.equals(module, that.module);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, module);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("exclude ");
        if (group != null) {
            sb.append("group: '").append(group).append("'");
            if (module != null) {
                sb.append(", ");
            }
        }
        if (module != null) {
            sb.append("module: '").append(module).append("'");
        }
        return sb.toString();
    }
}
