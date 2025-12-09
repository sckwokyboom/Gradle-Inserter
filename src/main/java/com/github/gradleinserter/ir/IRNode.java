package com.github.gradleinserter.ir;

/**
 * Base interface for all intermediate representation nodes.
 * IR nodes preserve source positions for accurate edit generation.
 */
public interface IRNode {

    /**
     * @return the start offset in source (inclusive)
     */
    int getStartOffset();

    /**
     * @return the end offset in source (exclusive)
     */
    int getEndOffset();

    /**
     * @return the original source text of this node
     */
    String getSourceText();

    /**
     * Accept a visitor for processing.
     */
    <T> T accept(IRNodeVisitor<T> visitor);
}
