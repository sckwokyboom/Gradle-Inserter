package com.github.gradleinserter.merge;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.view.SemanticView;

import java.util.List;

/**
 * Strategy for merging snippet views into original script views.
 *
 * @param <T> the type of semantic view this strategy handles
 */
public interface MergeStrategy<T extends SemanticView> {

    /**
     * @return the view type this strategy handles
     */
    SemanticView.ViewType getViewType();

    /**
     * Generate edits to merge snippet content into the original.
     *
     * @param original      the view from original script (may be null if block doesn't exist)
     * @param snippet       the view from snippet to merge
     * @param originalSource the full original script source
     * @param context       merge context with additional information
     * @return list of edits to apply
     */
    List<IInsertionEdit> merge(T original, T snippet, String originalSource, MergeContext context);

    /**
     * Check if this strategy can handle the given views.
     */
    default boolean canHandle(SemanticView original, SemanticView snippet) {
        return (original == null || original.getType() == getViewType())
                && snippet.getType() == getViewType();
    }
}
