package com.github.gradleinserter.ir;

/**
 * Represents raw/unparsed content that couldn't be mapped to semantic structure.
 * Used for preserving arbitrary Groovy code that we don't understand.
 */
public final class RawNode extends AbstractIRNode {

    public RawNode(int startOffset, int endOffset, String sourceText) {
        super(startOffset, endOffset, sourceText);
    }

    @Override
    public <T> T accept(IRNodeVisitor<T> visitor) {
        return visitor.visitRaw(this);
    }

    @Override
    public String toString() {
        String preview = getSourceText();
        if (preview.length() > 30) {
            preview = preview.substring(0, 30) + "...";
        }
        return "RawNode{'" + preview.replace("\n", "\\n") + "'}";
    }
}
