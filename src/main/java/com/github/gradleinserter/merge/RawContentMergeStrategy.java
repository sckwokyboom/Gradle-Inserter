package com.github.gradleinserter.merge;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.api.InsertEdit;
import com.github.gradleinserter.view.RawView;
import com.github.gradleinserter.view.SemanticView;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy for handling raw/unparsed content from snippets.
 *
 * Behavior:
 * - Appends raw content to the end of the script
 * - Preserves original content exactly
 */
public class RawContentMergeStrategy implements MergeStrategy<RawView> {

    @Override
    public SemanticView.ViewType getViewType() {
        return SemanticView.ViewType.RAW;
    }

    @Override
    public List<IInsertionEdit> merge(RawView original, RawView snippet,
                                      String originalSource, MergeContext context) {
        List<IInsertionEdit> edits = new ArrayList<>();

        String content = snippet.getContent().trim();
        if (!content.isEmpty()) {
            int insertPos = context.getNewBlockInsertionPoint();
            edits.add(new InsertEdit(insertPos, "\n" + content,
                    "Add raw content"));
        }

        return edits;
    }

    @Override
    public boolean canHandle(SemanticView original, SemanticView snippet) {
        // For raw content, we only care about the snippet type
        return snippet.getType() == SemanticView.ViewType.RAW;
    }
}
