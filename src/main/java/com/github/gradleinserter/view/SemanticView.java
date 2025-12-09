package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.IRNode;

/**
 * Base interface for semantic views of gradle script elements.
 * Views provide a high-level interpretation of IR nodes.
 */
public interface SemanticView {

    /**
     * @return the type of this semantic view
     */
    ViewType getType();

    /**
     * @return the underlying IR node (may be null for synthetic views)
     */
    IRNode getIRNode();

    /**
     * @return true if this view was derived from actual source (not synthetic)
     */
    default boolean hasSourceInfo() {
        return getIRNode() != null;
    }

    enum ViewType {
        DEPENDENCIES,
        PLUGINS,
        REPOSITORIES,
        JAVA_CONFIG,
        PROPERTY,
        UNKNOWN_BLOCK,
        RAW
    }
}
