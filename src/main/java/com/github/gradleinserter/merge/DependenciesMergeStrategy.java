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
            edits.add(new ReplaceEdit(insertPos, insertPos, prefix + newBlock,
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
            int versionIndex = existingSource.indexOf(existingVersion);
            if (versionIndex >= 0) {
                int absoluteStart = existing.getSourceNode().getStartOffset() + versionIndex;
                int absoluteEnd = absoluteStart + existingVersion.length();

                return new ReplaceEdit(absoluteStart, absoluteEnd, newVersion,
                        "Update version: " + existing.getCoordinateKey()
                                + " " + existingVersion + " -> " + newVersion);
            }
            return null;
        }

        // Case 2: Existing dependency has no version - add it
        return createVersionAdditionEdit(existing, newVersion);
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

        // Find excludes in snippet that don't exist in original
        List<ExcludeItem> newExcludes = new ArrayList<>();
        for (ExcludeItem snippetExclude : snippet.getExcludes()) {
            boolean exists = existing.getExcludes().stream()
                    .anyMatch(e -> e.matches(snippetExclude));
            if (!exists) {
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
        sb.append(context.getIndentation());
        // Use original source text from snippet to preserve notation style (map notation, quotes, etc.)
        sb.append(dep.getOriginalSourceText());
        sb.append("\n");
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
