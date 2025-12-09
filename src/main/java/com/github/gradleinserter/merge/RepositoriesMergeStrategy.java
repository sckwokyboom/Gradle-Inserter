package com.github.gradleinserter.merge;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.api.ReplaceEdit;
import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.view.RepositoriesView;
import com.github.gradleinserter.view.RepositoriesView.RepositoryItem;
import com.github.gradleinserter.view.SemanticView;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy for merging repositories blocks.
 *
 * Behavior:
 * - Adds new repositories that don't exist
 * - Does not remove or modify existing repositories
 */
public class RepositoriesMergeStrategy implements MergeStrategy<RepositoriesView> {

    @Override
    public SemanticView.ViewType getViewType() {
        return SemanticView.ViewType.REPOSITORIES;
    }

    @Override
    public List<IInsertionEdit> merge(RepositoriesView original, RepositoriesView snippet,
                                      String originalSource, MergeContext context) {
        List<IInsertionEdit> edits = new ArrayList<>();

        if (original == null || original.getBlockNode() == null) {
            // No repositories block - create one
            String newBlock = generateRepositoriesBlock(snippet, context);
            // Use semantic insertion point
            int insertPos = context.getSemanticInsertionPoint(SemanticView.ViewType.REPOSITORIES);
            String prefix = insertPos > 0 ? "\n\n" : "";
            edits.add(new ReplaceEdit(insertPos, insertPos, prefix + newBlock,
                    "Add repositories block"));
            return edits;
        }

        BlockNode originalBlock = original.getBlockNode();

        // Add repositories that don't exist
        for (RepositoryItem snippetRepo : snippet.getRepositories()) {
            if (!original.containsRepository(snippetRepo.getName())) {
                String newLine = formatRepositoryLine(snippetRepo, context);
                int insertPos = originalBlock.getBodyEndOffset();
                edits.add(new ReplaceEdit(insertPos, insertPos, newLine,
                        "Add repository: " + snippetRepo.getName()));
            }
        }

        return edits;
    }

    private String formatRepositoryLine(RepositoryItem repo, MergeContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getIndentation());

        if (repo.getUrl() != null) {
            sb.append("maven { url '").append(repo.getUrl()).append("' }");
        } else {
            sb.append(repo.getName()).append("()");
        }

        sb.append("\n");
        return sb.toString();
    }

    private String generateRepositoriesBlock(RepositoriesView snippet, MergeContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("repositories {");

        for (RepositoryItem repo : snippet.getRepositories()) {
            sb.append("\n").append(context.getIndentation());
            if (repo.getUrl() != null) {
                sb.append("maven { url '").append(repo.getUrl()).append("' }");
            } else {
                sb.append(repo.getName()).append("()");
            }
        }

        sb.append("\n}");
        return sb.toString();
    }
}
