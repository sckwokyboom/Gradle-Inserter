package com.github.gradleinserter.api;

import java.util.Objects;

/**
 * An edit that inserts text at a specific position (pure insertion, no deletion).
 */
public final class InsertEdit implements IInsertionEdit {

    private final int offset;
    private final String text;
    private final String description;

    public InsertEdit(int offset, String text) {
        this(offset, text, null);
    }

    public InsertEdit(int offset, String text, String description) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative: " + offset);
        }
        this.offset = offset;
        this.text = Objects.requireNonNull(text, "text");
        this.description = description;
    }

    @Override
    public int getStartOffset() {
        return offset;
    }

    @Override
    public int getEndOffset() {
        return offset;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getDescription() {
        return description != null ? description : "Insert at " + offset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InsertEdit)) return false;
        InsertEdit that = (InsertEdit) o;
        return offset == that.offset && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, text);
    }

    @Override
    public String toString() {
        return "InsertEdit{offset=" + offset + ", text='" + text + "'}";
    }
}
