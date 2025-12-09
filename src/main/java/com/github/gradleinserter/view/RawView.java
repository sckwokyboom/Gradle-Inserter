package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.IRNode;

/**
 * Semantic view for raw/unparsed content.
 */
public final class RawView implements SemanticView {

    private final IRNode node;

    public RawView(IRNode node) {
        this.node = node;
    }

    @Override
    public ViewType getType() {
        return ViewType.RAW;
    }

    @Override
    public IRNode getIRNode() {
        return node;
    }

    public String getContent() {
        return node.getSourceText();
    }

    @Override
    public String toString() {
        String preview = getContent();
        if (preview.length() > 30) {
            preview = preview.substring(0, 30) + "...";
        }
        return "RawView{'" + preview.replace("\n", "\\n") + "'}";
    }
}
