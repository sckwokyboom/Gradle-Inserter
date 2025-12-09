package com.github.gradleinserter.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a named block with a closure body.
 * Example: dependencies { ... }, plugins { ... }, subprojects { ... }
 */
public final class BlockNode extends AbstractIRNode {

    private final String name;
    private final List<IRNode> children;
    private final int bodyStartOffset;  // position right after opening '{'
    private final int bodyEndOffset;    // position of closing '}'

    public BlockNode(String name, int startOffset, int endOffset,
                     int bodyStartOffset, int bodyEndOffset,
                     List<IRNode> children, String sourceText) {
        super(startOffset, endOffset, sourceText);
        this.name = Objects.requireNonNull(name, "name");
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
        this.bodyStartOffset = bodyStartOffset;
        this.bodyEndOffset = bodyEndOffset;
    }

    public String getName() {
        return name;
    }

    public List<IRNode> getChildren() {
        return children;
    }

    /**
     * @return position right after the opening '{'
     */
    public int getBodyStartOffset() {
        return bodyStartOffset;
    }

    /**
     * @return position of the closing '}'
     */
    public int getBodyEndOffset() {
        return bodyEndOffset;
    }

    @Override
    public <T> T accept(IRNodeVisitor<T> visitor) {
        return visitor.visitBlock(this);
    }

    @Override
    public String toString() {
        return "BlockNode{name='" + name + "', children=" + children.size() + "}";
    }
}
