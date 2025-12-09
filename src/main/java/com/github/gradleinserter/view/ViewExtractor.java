package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts semantic views from IR nodes.
 */
public class ViewExtractor {

    /**
     * Extract semantic views from a list of IR nodes.
     */
    public List<SemanticView> extract(List<IRNode> nodes) {
        List<SemanticView> views = new ArrayList<>();

        for (IRNode node : nodes) {
            SemanticView view = extractSingle(node);
            if (view != null) {
                views.add(view);
            }
        }

        return views;
    }

    /**
     * Extract a single semantic view from an IR node.
     */
    public SemanticView extractSingle(IRNode node) {
        if (node instanceof BlockNode) {
            return extractFromBlock((BlockNode) node);
        }
        if (node instanceof PropertyNode) {
            return new PropertyView((PropertyNode) node);
        }
        if (node instanceof MethodCallNode) {
            // Standalone method calls might be dependency declarations in snippets
            return new RawView(node);
        }
        if (node instanceof RawNode) {
            return new RawView(node);
        }
        return null;
    }

    private SemanticView extractFromBlock(BlockNode block) {
        String name = block.getName();

        switch (name) {
            case "dependencies":
                return new DependenciesView(block);
            case "plugins":
                return new PluginsView(block);
            case "repositories":
                return new RepositoriesView(block);
            default:
                return new UnknownBlockView(block);
        }
    }
}
