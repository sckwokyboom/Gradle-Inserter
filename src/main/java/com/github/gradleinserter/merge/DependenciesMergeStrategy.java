package com.github.gradleinserter.merge;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.api.InsertEdit;
import com.github.gradleinserter.api.ReplaceEdit;
import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.view.DependenciesView;
import com.github.gradleinserter.view.DependencyItem;
import com.github.gradleinserter.view.SemanticView;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy for merging dependencies blocks.
 *
 * Behavior:
 * - Updates versions of existing dependencies
 * - Adds new dependencies that don't exist
 * - Preserves original formatting and order
 */
public class DependenciesMergeStrategy implements MergeStrategy<DependenciesView> {

    @Override
    public SemanticView.ViewType getViewType() {
        return SemanticView.ViewType.DEPENDENCIES;
    }

    @Override
    public List<IInsertionEdit> merge(DependenciesView original, DependenciesView snippet,
                                      String originalSource, MergeContext context) {
        List<IInsertionEdit> edits = new ArrayList<>();

        if (original == null || original.getBlockNode() == null) {
            // No dependencies block in original - need to create one
            String newBlock = generateDependenciesBlock(snippet, context);
            int insertPos = context.getNewBlockInsertionPoint();
            edits.add(new InsertEdit(insertPos, "\n" + newBlock,
                    "Add dependencies block"));
            return edits;
        }

        BlockNode originalBlock = original.getBlockNode();

        // Process each snippet dependency
        for (DependencyItem snippetDep : snippet.getDependencies()) {
            List<DependencyItem> existingDeps = original.findByArtifact(snippetDep);

            if (existingDeps.isEmpty()) {
                // New dependency - add it
                String newLine = formatDependencyLine(snippetDep, context);
                int insertPos = originalBlock.getBodyEndOffset();
                edits.add(new InsertEdit(insertPos, newLine,
                        "Add dependency: " + snippetDep.getFullCoordinate()));
            } else {
                // Update existing dependencies with new version
                for (DependencyItem existing : existingDeps) {
                    if (needsVersionUpdate(existing, snippetDep)) {
                        IInsertionEdit edit = createVersionUpdateEdit(existing, snippetDep, originalSource);
                        if (edit != null) {
                            edits.add(edit);
                        }
                    }
                }
            }
        }

        return edits;
    }

    private boolean needsVersionUpdate(DependencyItem existing, DependencyItem snippet) {
        String existingVersion = existing.getVersion();
        String snippetVersion = snippet.getVersion();

        if (snippetVersion == null || snippetVersion.isEmpty()) {
            return false;
        }

        return !snippetVersion.equals(existingVersion);
    }

    private IInsertionEdit createVersionUpdateEdit(DependencyItem existing,
                                                    DependencyItem snippet,
                                                    String originalSource) {
        if (existing.getSourceNode() == null) {
            return null;
        }

        String existingSource = existing.getSourceNode().getSourceText();
        String existingVersion = existing.getVersion();
        String newVersion = snippet.getVersion();

        if (existingVersion == null || newVersion == null) {
            return null;
        }

        // Find the version in the source and replace it
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

    private String formatDependencyLine(DependencyItem dep, MergeContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(context.getIndentation());
        sb.append(dep.getConfiguration()).append(" '").append(dep.getFullCoordinate()).append("'");
        return sb.toString();
    }

    private String generateDependenciesBlock(DependenciesView snippet, MergeContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("dependencies {");

        for (DependencyItem dep : snippet.getDependencies()) {
            sb.append("\n").append(context.getIndentation());
            sb.append(dep.getConfiguration()).append(" '").append(dep.getFullCoordinate()).append("'");
        }

        sb.append("\n}");
        return sb.toString();
    }
}
