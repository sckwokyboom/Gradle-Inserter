package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.ir.IRNode;

/**
 * Semantic view for blocks that we don't have special handling for.
 * Examples: buildscript, allprojects, subprojects, task definitions, etc.
 */
public final class UnknownBlockView implements SemanticView {

    private final BlockNode blockNode;

    public UnknownBlockView(BlockNode blockNode) {
        this.blockNode = blockNode;
    }

    @Override
    public ViewType getType() {
        return ViewType.UNKNOWN_BLOCK;
    }

    @Override
    public IRNode getIRNode() {
        return blockNode;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public String getBlockName() {
        return blockNode.getName();
    }

    @Override
    public String toString() {
        return "UnknownBlockView{name='" + getBlockName() + "'}";
    }
}
