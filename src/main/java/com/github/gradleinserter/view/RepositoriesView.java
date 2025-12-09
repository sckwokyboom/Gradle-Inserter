package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.ir.IRNode;
import com.github.gradleinserter.ir.MethodCallNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            } else if (child instanceof BlockNode) {
                // Handle nested blocks like: maven { url = '...' }
                BlockNode nestedBlock = (BlockNode) child;
                result.add(RepositoryItem.fromBlockNode(nestedBlock));
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
        @NotNull
        private final String name;
        @Nullable
        private final String url;  // may be null for predefined repos
        @Nullable
        private final IRNode sourceNode; // can be MethodCallNode or BlockNode

        public RepositoryItem(@NotNull String name, @Nullable String url, @Nullable IRNode sourceNode) {
            this.name = Objects.requireNonNull(name, "name");
            this.url = url;
            this.sourceNode = sourceNode;
        }

        @NotNull
        public static RepositoryItem fromMethodCall(@NotNull MethodCallNode node) {
            String name = node.getMethodName();
            String url = null;

            // For maven { url 'xxx' } blocks
            if (node.hasClosureBody()) {
                // Extract URL from closure if present
                String source = node.getSourceText();
                url = extractUrlFromSource(source);
            } else if (!node.getArguments().isEmpty()) {
                url = node.getFirstArgument();
            }

            return new RepositoryItem(name, url, node);
        }

        @NotNull
        public static RepositoryItem fromBlockNode(@NotNull BlockNode node) {
            String name = node.getName();
            String source = node.getSourceText();
            String url = extractUrlFromSource(source);
            return new RepositoryItem(name, url, node);
        }

        @Nullable
        private static String extractUrlFromSource(@Nullable String source) {
            if (source == null) {
                return null;
            }
            int urlIdx = source.indexOf("url");
            if (urlIdx >= 0) {
                // Handle url = uri('...') pattern
                int uriIdx = source.indexOf("uri(", urlIdx);
                if (uriIdx >= 0) {
                    int start = source.indexOf("'", uriIdx);
                    if (start < 0) start = source.indexOf("\"", uriIdx);
                    if (start >= 0) {
                        int end = source.indexOf(source.charAt(start), start + 1);
                        if (end > start) {
                            return source.substring(start + 1, end);
                        }
                    }
                }
                // Handle url = '...' or url '...' patterns
                int start = source.indexOf("'", urlIdx);
                if (start < 0) start = source.indexOf("\"", urlIdx);
                if (start >= 0) {
                    int end = source.indexOf(source.charAt(start), start + 1);
                    if (end > start) {
                        return source.substring(start + 1, end);
                    }
                }
            }
            return null;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @Nullable
        public String getUrl() {
            return url;
        }

        @Nullable
        public IRNode getSourceNode() {
            return sourceNode;
        }

        /**
         * Get the original source text for this repository, preserving all nested properties.
         * Useful for complex repositories with allowInsecureProtocol, credentials, etc.
         *
         * @return the original source text, or a reconstructed string if source is not available
         */
        @NotNull
        public String getOriginalSourceText() {
            if (sourceNode != null) {
                String sourceText = sourceNode.getSourceText();
                if (sourceText != null && !sourceText.isEmpty()) {
                    return sourceText;
                }
            }
            // Fallback to simple reconstruction
            if (url != null) {
                return name + " { url '" + url + "' }";
            }
            return name + "()";
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
