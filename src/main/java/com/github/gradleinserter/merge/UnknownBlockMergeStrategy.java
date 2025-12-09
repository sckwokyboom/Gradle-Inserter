package com.github.gradleinserter.merge;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.api.InsertEdit;
import com.github.gradleinserter.api.ReplaceEdit;
import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.ir.IRNode;
import com.github.gradleinserter.view.SemanticView;
import com.github.gradleinserter.view.UnknownBlockView;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy for merging unknown/generic blocks.
 *
 * Behavior:
 * - If block with same name exists, append content to it
 * - If block doesn't exist, add it to the end
 */
public class UnknownBlockMergeStrategy implements MergeStrategy<UnknownBlockView> {

    @Override
    public SemanticView.ViewType getViewType() {
        return SemanticView.ViewType.UNKNOWN_BLOCK;
    }

    @Override
    public List<IInsertionEdit> merge(UnknownBlockView original, UnknownBlockView snippet,
                                      String originalSource, MergeContext context) {
        List<IInsertionEdit> edits = new ArrayList<>();

        if (original == null || original.getBlockNode() == null) {
            // Block doesn't exist - add it
            String newBlock = snippet.getBlockNode().getSourceText();
            int insertPos = context.getNewBlockInsertionPoint();
            edits.add(new InsertEdit(insertPos, "\n" + newBlock,
                    "Add block: " + snippet.getBlockName()));
            return edits;
        }

        // Block exists - append snippet's children content
        BlockNode originalBlock = original.getBlockNode();
        BlockNode snippetBlock = snippet.getBlockNode();

        // Extract the body content from snippet (everything between { and })
        String snippetContent = extractBodyContent(snippetBlock, context);
        if (!snippetContent.trim().isEmpty()) {
            int insertPos = originalBlock.getBodyEndOffset();
            edits.add(new InsertEdit(insertPos, snippetContent,
                    "Append to block: " + snippet.getBlockName()));
        }

        return edits;
    }

    private String extractBodyContent(BlockNode block, MergeContext context) {
        StringBuilder sb = new StringBuilder();
        for (IRNode child : block.getChildren()) {
            sb.append("\n").append(context.getIndentation());
            sb.append(child.getSourceText().trim());
        }
        return sb.toString();
    }

    /**
     * Find an unknown block by name in the original views.
     */
    public static UnknownBlockView findByName(List<SemanticView> views, String blockName) {
        for (SemanticView view : views) {
            if (view instanceof UnknownBlockView) {
                UnknownBlockView blockView = (UnknownBlockView) view;
                if (blockView.getBlockName().equals(blockName)) {
                    return blockView;
                }
            }
        }
        return null;
    }
}
