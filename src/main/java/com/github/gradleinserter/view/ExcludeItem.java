package com.github.gradleinserter.view;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Parse an exclude item from source text.
     * Handles formats like:
     * - exclude group: 'com.example', module: 'some-module'
     * - exclude group: "com.example"
     * - exclude module: 'some-module'
     */
    @NotNull
    public static ExcludeItem parse(@NotNull String source) {
        String group = extractMapValue(source, "group");
        String module = extractMapValue(source, "module");
        return new ExcludeItem(
                group.isEmpty() ? null : group,
                module.isEmpty() ? null : module,
                source.trim()
        );
    }

    @NotNull
    private static String extractMapValue(@NotNull String source, @NotNull String key) {
        Pattern pattern = Pattern.compile(key + "\\s*:\\s*['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
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
