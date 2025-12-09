package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.ir.IRNode;
import com.github.gradleinserter.ir.MethodCallNode;

import java.util.*;

/**
 * Semantic view of a repositories block.
 */
public final class RepositoriesView implements SemanticView {

    private final BlockNode blockNode;
    private final List<RepositoryItem> repositories;

    public RepositoriesView(BlockNode blockNode) {
        this.blockNode = blockNode;
        this.repositories = extractRepositories(blockNode);
    }

    private List<RepositoryItem> extractRepositories(BlockNode block) {
        List<RepositoryItem> result = new ArrayList<>();
        for (IRNode child : block.getChildren()) {
            if (child instanceof MethodCallNode) {
                MethodCallNode methodCall = (MethodCallNode) child;
                result.add(RepositoryItem.fromMethodCall(methodCall));
            }
        }
        return result;
    }

    @Override
    public ViewType getType() {
        return ViewType.REPOSITORIES;
    }

    @Override
    public IRNode getIRNode() {
        return blockNode;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public List<RepositoryItem> getRepositories() {
        return Collections.unmodifiableList(repositories);
    }

    public boolean containsRepository(String name) {
        return repositories.stream().anyMatch(r -> r.getName().equals(name));
    }

    @Override
    public String toString() {
        return "RepositoriesView{repositories=" + repositories.size() + "}";
    }

    /**
     * Represents a single repository declaration.
     */
    public static final class RepositoryItem {
        private final String name;
        private final String url;  // may be null for predefined repos
        private final MethodCallNode sourceNode;

        public RepositoryItem(String name, String url, MethodCallNode sourceNode) {
            this.name = Objects.requireNonNull(name, "name");
            this.url = url;
            this.sourceNode = sourceNode;
        }

        public static RepositoryItem fromMethodCall(MethodCallNode node) {
            String name = node.getMethodName();
            String url = null;

            // For maven { url 'xxx' } blocks
            if (node.hasClosureBody()) {
                // Extract URL from closure if present
                String source = node.getSourceText();
                int urlIdx = source.indexOf("url");
                if (urlIdx >= 0) {
                    int start = source.indexOf("'", urlIdx);
                    if (start < 0) start = source.indexOf("\"", urlIdx);
                    if (start >= 0) {
                        int end = source.indexOf(source.charAt(start), start + 1);
                        if (end > start) {
                            url = source.substring(start + 1, end);
                        }
                    }
                }
            } else if (!node.getArguments().isEmpty()) {
                url = node.getFirstArgument();
            }

            return new RepositoryItem(name, url, node);
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public MethodCallNode getSourceNode() {
            return sourceNode;
        }

        @Override
        public String toString() {
            if (url != null) {
                return name + " { url '" + url + "' }";
            }
            return name + "()";
        }
    }
}
