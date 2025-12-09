package com.github.gradleinserter.api;

import java.util.Objects;

/**
 * An edit that replaces a range of text with new text.
 */
public final class ReplaceEdit implements IInsertionEdit {

    private final int startOffset;
    private final int endOffset;
    private final String text;
    private final String description;

    public ReplaceEdit(int startOffset, int endOffset, String text) {
        this(startOffset, endOffset, text, null);
    }

    public ReplaceEdit(int startOffset, int endOffset, String text, String description) {
        if (startOffset < 0) {
            throw new IllegalArgumentException("Start offset must be non-negative: " + startOffset);
        }
        if (endOffset < startOffset) {
            throw new IllegalArgumentException("End offset must be >= start offset: " + endOffset + " < " + startOffset);
        }
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.text = Objects.requireNonNull(text, "text");
        this.description = description;
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
    public String getText() {
        return text;
    }

    @Override
    public String getDescription() {
        return description != null ? description
                : "Replace [" + startOffset + ", " + endOffset + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplaceEdit)) return false;
        ReplaceEdit that = (ReplaceEdit) o;
        return startOffset == that.startOffset
                && endOffset == that.endOffset
                && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startOffset, endOffset, text);
    }

    @Override
    public String toString() {
        return "ReplaceEdit{startOffset=" + startOffset
                + ", endOffset=" + endOffset
                + ", text='" + text + "'}";
    }
}
