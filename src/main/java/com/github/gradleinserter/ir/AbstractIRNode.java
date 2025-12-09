package com.github.gradleinserter.ir;

import java.util.Objects;

/**
 * Base implementation of IRNode with common functionality.
 */
public abstract class AbstractIRNode implements IRNode {

    private final int startOffset;
    private final int endOffset;
    private final String sourceText;

    protected AbstractIRNode(int startOffset, int endOffset, String sourceText) {
        if (startOffset < 0) {
            throw new IllegalArgumentException("Start offset must be non-negative: " + startOffset);
        }
        if (endOffset < startOffset) {
            throw new IllegalArgumentException("End offset must be >= start: " + endOffset + " < " + startOffset);
        }
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.sourceText = Objects.requireNonNull(sourceText, "sourceText");
    }

    @Override
    public int getStartOffset() {
        return startOffset;
    }

    @Override
    public int getEndOffset() {
        return endOffset;
    }

    @Override
    public String getSourceText() {
        return sourceText;
    }
}
