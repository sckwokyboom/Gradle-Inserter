package com.github.gradleinserter.ir;

/**
 * Visitor pattern for IR nodes.
 */
public interface IRNodeVisitor<T> {

    T visitBlock(BlockNode node);

    T visitMethodCall(MethodCallNode node);

    T visitProperty(PropertyNode node);

    T visitRaw(RawNode node);

    /**
     * Default implementation that returns null for unknown nodes.
     */
    default T visitUnknown(IRNode node) {
        return null;
    }
}
