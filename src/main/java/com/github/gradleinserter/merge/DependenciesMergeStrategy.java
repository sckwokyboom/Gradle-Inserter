package com.github.gradleinserter.merge;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.api.ReplaceEdit;
import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.view.DependenciesView;
import com.github.gradleinserter.view.DependencyItem;
import com.github.gradleinserter.view.ExcludeItem;
import com.github.gradleinserter.view.SemanticView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Strategy for merging dependencies blocks.
 *
 * Behavior:
 * - Updates versions of existing dependencies
 * - Adds new dependencies that don't exist
 * - Preserves original formatting and order
 */
public class DependenciesMergeStrategy implements MergeStrategy<DependenciesView> {

    @NotNull
    @Override
    public SemanticView.ViewType getViewType() {
        return SemanticView.ViewType.DEPENDENCIES;
    }

    @NotNull
    @Override
    public List<IInsertionEdit> merge(@Nullable DependenciesView original, @NotNull DependenciesView snippet,
                                      @NotNull String originalSource, @NotNull MergeContext context) {
        List<IInsertionEdit> edits = new ArrayList<>();

        if (original == null || original.getBlockNode() == null) {
            // No dependencies block in original - need to create one
            String newBlock = generateDependenciesBlock(snippet, context);
            int insertPos = context.getSemanticInsertionPoint(SemanticView.ViewType.DEPENDENCIES);
            String prefix = insertPos > 0 ? "\n\n" : "";
            String suffix = "\n";
            edits.add(new ReplaceEdit(insertPos, insertPos, prefix + newBlock + suffix,
                    "Add dependencies block"));
            return edits;
        }

        BlockNode originalBlock = original.getBlockNode();

        // Process each snippet dependency
        for (DependencyItem snippetDep : snippet.getDependencies()) {
            // Use configuration + artifact matching - testImplementation and implementation
            // for the same artifact are treated as different dependencies
            Optional<DependencyItem> existingDep = original.findByConfigurationAndArtifact(snippetDep);

            if (existingDep.isEmpty()) {
                // New dependency (different configuration or completely new artifact) - add it
                String newLine = formatDependencyLine(snippetDep, context);
                int insertPos = originalBlock.getBodyEndOffset();
                edits.add(new ReplaceEdit(insertPos, insertPos, newLine,
                        "Add dependency: " + snippetDep.getConfiguration() + " " + snippetDep.getFullCoordinate()));
            } else {
                // Update existing dependency with new version (same configuration + artifact)
                DependencyItem existing = existingDep.get();
                if (needsVersionUpdate(existing, snippetDep)) {
                    IInsertionEdit edit = createVersionUpdateEdit(existing, snippetDep, originalSource);
                    if (edit != null) {
                        edits.add(edit);
                    }
                }

                // Merge excludes from snippet into existing dependency
                if (snippetDep.hasExcludes()) {
                    List<IInsertionEdit> excludeEdits = createExcludesAdditionEdits(existing, snippetDep, context);
                    edits.addAll(excludeEdits);
                }
            }
        }

        return edits;
    }

    private boolean needsVersionUpdate(@NotNull DependencyItem existing, @NotNull DependencyItem snippet) {
        String existingVersion = existing.getVersion();
        String snippetVersion = snippet.getVersion();

        if (snippetVersion == null || snippetVersion.isEmpty()) {
            return false;
        }

        return !snippetVersion.equals(existingVersion);
    }

    @Nullable
    private IInsertionEdit createVersionUpdateEdit(@NotNull DependencyItem existing,
                                                    @NotNull DependencyItem snippet,
                                                    @NotNull String originalSource) {
        if (existing.getSourceNode() == null) {
            return null;
        }

        String existingSource = existing.getSourceNode().getSourceText();
        String existingVersion = existing.getVersion();
        String newVersion = snippet.getVersion();

        if (newVersion == null || newVersion.isEmpty()) {
            return null;
        }

        // Case 1: Existing dependency has a version - replace it
        if (existingVersion != null && !existingVersion.isEmpty()) {
            // Find the version in the source, handling both plain versions and property references
            // We need to find the actual version text in the source (which might include ${...} or $...)
            VersionLocation versionLoc = findVersionInSource(existingSource, existingVersion);
            if (versionLoc != null) {
                int absoluteStart = existing.getSourceNode().getStartOffset() + versionLoc.startIndex;
                int absoluteEnd = absoluteStart + versionLoc.length;

                return new ReplaceEdit(absoluteStart, absoluteEnd, newVersion,
                        "Update version: " + existing.getCoordinateKey()
                                + " " + existingVersion + " -> " + newVersion);
            }
            return null;
        }

        // Case 2: Existing dependency has no version - add it
        return createVersionAdditionEdit(existing, newVersion);
    }

    /**
     * Find the version position and length in the source text.
     * Handles both plain versions "1.0" and property references "${version}", "$version", etc.
     *
     * @return VersionLocation with start index and length, or null if not found
     */
    @Nullable
    private VersionLocation findVersionInSource(@NotNull String source, @NotNull String version) {
        // First try exact match
        int index = source.indexOf(version);
        if (index >= 0) {
            return new VersionLocation(index, version.length());
        }

        // Version is complex - find it by locating the version position in the coordinate string
        // Look for the second colon that separates artifactId from version
        int versionStart = findVersionStartInSource(source);
        if (versionStart >= 0) {
            int versionEnd = findVersionEndInSource(source, versionStart);
            if (versionEnd > versionStart) {
                return new VersionLocation(versionStart, versionEnd - versionStart);
            }
        }

        return null;
    }

    /**
     * Find where the version starts in the source (after the second colon).
     */
    private int findVersionStartInSource(@NotNull String source) {
        int colonCount = 0;
        int braceDepth = 0;
        int bracketDepth = 0;
        int parenDepth = 0;

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);

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
                    if (braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                        colonCount++;
                        if (colonCount == 2) {
                            return i + 1; // Start right after the second colon
                        }
                    }
                    break;
            }
        }

        return -1;
    }

    /**
     * Find where the version ends in the source.
     * Must handle nested expressions like ${props['key']}.
     *
     * Strategy: Track depth of braces/brackets/parens. The version ends when we hit
     * a quote, closing paren at depth 0, or classifier marker.
     */
    private int findVersionEndInSource(@NotNull String source, int versionStart) {
        int braceDepth = 0;
        int bracketDepth = 0;
        int parenDepth = 0;
        boolean inDollarExpr = false;

        for (int i = versionStart; i < source.length(); i++) {
            char c = source.charAt(i);

            // Track $ expressions
            if (c == '$' && i + 1 < source.length()) {
                char next = source.charAt(i + 1);
                if (next == '{') {
                    inDollarExpr = true;
                }
            }

            switch (c) {
                case '{':
                    braceDepth++;
                    break;
                case '}':
                    if (braceDepth > 0) {
                        braceDepth--;
                        if (braceDepth == 0 && inDollarExpr) {
                            inDollarExpr = false;
                        }
                    }
                    break;
                case '[':
                    bracketDepth++;
                    break;
                case ']':
                    if (bracketDepth > 0) {
                        bracketDepth--;
                    }
                    break;
                case '(':
                    parenDepth++;
                    break;
                case ')':
                    if (parenDepth > 0) {
                        parenDepth--;
                    } else {
                        // End of version - found closing paren of dependency notation
                        return i;
                    }
                    break;
                case '\'':
                case '"':
                    // End of version - found closing quote
                    if (braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                        return i;
                    }
                    break;
                case '@':
                    // Classifier separator
                    if (braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                        return i;
                    }
                    break;
            }
        }

        return source.length();
    }

    /**
     * Helper class for version location in source.
     */
    private static class VersionLocation {
        final int startIndex;
        final int length;

        VersionLocation(int startIndex, int length) {
            this.startIndex = startIndex;
            this.length = length;
        }
    }

    @Nullable
    private IInsertionEdit createVersionAdditionEdit(@NotNull DependencyItem existing,
                                                     @NotNull String newVersion) {
        if (existing.getSourceNode() == null) {
            return null;
        }

        String existingSource = existing.getSourceNode().getSourceText();
        String artifactName = existing.getName();

        // For string notation like 'group:name', add :version after name
        // Look for the artifact name followed by quote
        int nameIndex = existingSource.lastIndexOf(artifactName);
        if (nameIndex >= 0) {
            int insertPos = nameIndex + artifactName.length();
            // Check what character follows - should be a quote or end of coordinate
            if (insertPos < existingSource.length()) {
                char nextChar = existingSource.charAt(insertPos);
                if (nextChar == '\'' || nextChar == '"') {
                    int absolutePos = existing.getSourceNode().getStartOffset() + insertPos;
                    return new ReplaceEdit(absolutePos, absolutePos, ":" + newVersion,
                            "Add version: " + existing.getCoordinateKey() + " -> " + newVersion);
                }
            }
        }

        // For map notation, would need to add 'version: ...' - more complex
        // For now, skip map notation without version
        return null;
    }

    @NotNull
    private List<IInsertionEdit> createExcludesAdditionEdits(@NotNull DependencyItem existing,
                                                              @NotNull DependencyItem snippet,
                                                              @NotNull MergeContext context) {
        List<IInsertionEdit> edits = new ArrayList<>();

        if (existing.getSourceNode() == null || !snippet.hasExcludes()) {
            return edits;
        }

        // Strategy: Snippet excludes take priority. We need to:
        // 1. Add excludes from snippet that aren't covered by existing excludes
        // 2. Remove existing excludes that conflict with snippet excludes (broader vs narrower)

        // First, identify excludes from snippet that need to be added or replace existing ones
        List<ExcludeItem> excludesToAdd = new ArrayList<>();
        List<ExcludeItem> existingExcludesToRemove = new ArrayList<>();

        for (ExcludeItem snippetExclude : snippet.getExcludes()) {
            boolean covered = false;
            boolean replacesExisting = false;

            for (ExcludeItem existingExclude : existing.getExcludes()) {
                if (existingExclude.covers(snippetExclude)) {
                    // Existing exclude already covers this snippet exclude
                    covered = true;
                    break;
                }
                if (snippetExclude.covers(existingExclude)) {
                    // Snippet exclude is broader and should replace existing
                    replacesExisting = true;
                    existingExcludesToRemove.add(existingExclude);
                }
            }

            if (!covered) {
                excludesToAdd.add(snippetExclude);
            }
        }

        // For now, we'll just add new excludes (removing existing excludes is complex and risky)
        // But we won't add if they're already covered by existing broader excludes
        List<ExcludeItem> newExcludes = new ArrayList<>();
        for (ExcludeItem snippetExclude : excludesToAdd) {
            boolean alreadyCovered = existing.getExcludes().stream()
                    .anyMatch(e -> e.covers(snippetExclude));
            if (!alreadyCovered) {
                newExcludes.add(snippetExclude);
            }
        }

        if (newExcludes.isEmpty()) {
            return edits;
        }

        String existingSource = existing.getSourceNode().getSourceText();
        int nodeEnd = existing.getSourceNode().getEndOffset();

        // Check if existing dependency already has a closure (look for { in source)
        boolean hasClosure = existingSource.contains("{");

        if (hasClosure) {
            // Add excludes before the closing brace of existing closure
            int closeBrace = existingSource.lastIndexOf('}');
            if (closeBrace >= 0) {
                int insertPos = existing.getSourceNode().getStartOffset() + closeBrace;
                StringBuilder sb = new StringBuilder();
                for (ExcludeItem exclude : newExcludes) {
                    sb.append("\n").append(context.getIndentation()).append(context.getIndentation())
                            .append(exclude.getRawSource());
                }
                sb.append("\n").append(context.getIndentation());
                edits.add(new ReplaceEdit(insertPos, insertPos, sb.toString(),
                        "Add excludes to " + existing.getCoordinateKey()));
            }
        } else {
            // Need to add closure with excludes
            // Find the end of the dependency line (before any trailing newline)
            int insertPos = nodeEnd;
            // Look backwards from end for the actual content end (before newline)
            String originalFull = existing.getSourceNode().getSourceText();
            while (insertPos > existing.getSourceNode().getStartOffset()
                   && (originalFull.charAt(insertPos - existing.getSourceNode().getStartOffset() - 1) == '\n'
                       || originalFull.charAt(insertPos - existing.getSourceNode().getStartOffset() - 1) == ' ')) {
                insertPos--;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(" {\n");
            for (ExcludeItem exclude : newExcludes) {
                sb.append(context.getIndentation()).append(context.getIndentation())
                        .append(exclude.getRawSource()).append("\n");
            }
            sb.append(context.getIndentation()).append("}");

            edits.add(new ReplaceEdit(insertPos, insertPos, sb.toString(),
                    "Add excludes to " + existing.getCoordinateKey()));
        }

        return edits;
    }

    @NotNull
    private String formatDependencyLine(@NotNull DependencyItem dep, @NotNull MergeContext context) {
        StringBuilder sb = new StringBuilder();
        String sourceText = dep.getOriginalSourceText();

        // If the dependency has multiple lines (e.g., with excludes), we need to adjust indentation
        if (sourceText.contains("\n")) {
            // Multi-line dependency - need to re-indent each line
            String[] lines = sourceText.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                String line = lines[i];
                // Trim leading whitespace and add proper indentation
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    if (i == 0) {
                        // First line: base indentation (one level for dependencies block)
                        sb.append(context.getIndentation()).append(trimmed);
                    } else if (trimmed.equals("}")) {
                        // Closing brace: same indentation as opening line
                        sb.append(context.getIndentation()).append(trimmed);
                    } else {
                        // Inner lines (excludes): two levels of indentation
                        sb.append(context.getIndentation()).append(context.getIndentation()).append(trimmed);
                    }
                }
            }
            sb.append("\n");
        } else {
            // Single-line dependency
            sb.append(context.getIndentation());
            sb.append(sourceText);
            sb.append("\n");
        }
        return sb.toString();
    }

    @NotNull
    private String generateDependenciesBlock(@NotNull DependenciesView snippet, @NotNull MergeContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("dependencies {");

        for (DependencyItem dep : snippet.getDependencies()) {
            sb.append("\n").append(context.getIndentation());
            // Use original source text from snippet to preserve notation style
            sb.append(dep.getOriginalSourceText());
        }

        sb.append("\n}");
        return sb.toString();
    }
}
