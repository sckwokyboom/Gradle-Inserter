package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.ir.IRNode;
import com.github.gradleinserter.ir.MethodCallNode;

import java.util.*;

/**
 * Semantic view of a plugins block.
 */
public final class PluginsView implements SemanticView {

    private final BlockNode blockNode;
    private final List<PluginItem> plugins;

    public PluginsView(BlockNode blockNode) {
        this.blockNode = blockNode;
        this.plugins = extractPlugins(blockNode);
    }

    /**
     * Create a synthetic view from method calls (snippet without block wrapper).
     */
    public PluginsView(List<MethodCallNode> calls) {
        this.blockNode = null;
        this.plugins = new ArrayList<>();
        for (MethodCallNode call : calls) {
            plugins.add(PluginItem.fromMethodCall(call));
        }
    }

    private List<PluginItem> extractPlugins(BlockNode block) {
        List<PluginItem> result = new ArrayList<>();
        for (IRNode child : block.getChildren()) {
            if (child instanceof MethodCallNode) {
                MethodCallNode methodCall = (MethodCallNode) child;
                result.add(PluginItem.fromMethodCall(methodCall));
            }
        }
        return result;
    }

    @Override
    public ViewType getType() {
        return ViewType.PLUGINS;
    }

    @Override
    public IRNode getIRNode() {
        return blockNode;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public List<PluginItem> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    /**
     * Find plugin by ID.
     */
    public Optional<PluginItem> findById(String id) {
        return plugins.stream()
                .filter(p -> Objects.equals(p.getId(), id))
                .findFirst();
    }

    @Override
    public String toString() {
        return "PluginsView{plugins=" + plugins.size() + "}";
    }
}
