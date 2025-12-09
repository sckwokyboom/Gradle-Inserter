package com.github.gradleinserter.api;

/**
 * Represents an edit operation to be applied to the original script.
 * Edits are position-based patches that can be applied sequentially
 * (from end to start to preserve offsets).
 */
public interface IInsertionEdit {

    /**
     * @return start offset in the original script (inclusive)
     */
    int getStartOffset();

    /**
     * @return end offset in the original script (exclusive)
     */
    int getEndOffset();

    /**
     * @return text to insert at the position. For pure insertions,
     *         startOffset == endOffset. For replacements, the range
     *         [startOffset, endOffset) is replaced with this text.
     */
    String getText();

    /**
     * @return description of what this edit does (for debugging/UI)
     */
    default String getDescription() {
        return "Edit at [" + getStartOffset() + ", " + getEndOffset() + ")";
    }
}
