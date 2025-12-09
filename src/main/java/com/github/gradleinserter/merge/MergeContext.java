package com.github.gradleinserter.merge;

import com.github.gradleinserter.view.SemanticView;

import java.util.List;
import java.util.Optional;

/**
 * Context for merge operations, providing access to script-level information.
 */
public final class MergeContext {

    private final String originalSource;
    private final List<SemanticView> originalViews;
    private final String indentation;

    public MergeContext(String originalSource, List<SemanticView> originalViews) {
        this(originalSource, originalViews, detectIndentation(originalSource));
    }

    public MergeContext(String originalSource, List<SemanticView> originalViews, String indentation) {
        this.originalSource = originalSource;
        this.originalViews = originalViews;
        this.indentation = indentation;
    }

    public String getOriginalSource() {
        return originalSource;
    }

    public List<SemanticView> getOriginalViews() {
        return originalViews;
    }

    /**
     * @return the detected/configured indentation string (e.g., "    " or "\t")
     */
    public String getIndentation() {
        return indentation;
    }

    /**
     * Find a view of specific type in original script.
     */
    @SuppressWarnings("unchecked")
    public <T extends SemanticView> Optional<T> findOriginalView(SemanticView.ViewType type) {
        return originalViews.stream()
                .filter(v -> v.getType() == type)
                .map(v -> (T) v)
                .findFirst();
    }

    /**
     * Get the position for inserting a new top-level block.
     * Typically at the end of the script.
     */
    public int getNewBlockInsertionPoint() {
        return originalSource.length();
    }

    /**
     * Get the position for inserting a block according to Gradle semantic ordering.
     * Order: plugins → properties → repositories → dependencies → other blocks
     *
     * @param blockType the type of block to insert
     * @return the appropriate insertion position
     */
    public int getSemanticInsertionPoint(SemanticView.ViewType blockType) {
        switch (blockType) {
            case PLUGINS:
                // Plugins should be first
                return 0;

            case PROPERTY:
                // Properties go after plugins
                return findInsertionPointAfter(SemanticView.ViewType.PLUGINS)
                        .orElse(0);

            case REPOSITORIES:
                // Repositories go after plugins and properties
                return findInsertionPointAfterLast(
                        SemanticView.ViewType.PLUGINS,
                        SemanticView.ViewType.PROPERTY
                ).orElse(0);

            case DEPENDENCIES:
                // Dependencies go after plugins, properties, and repositories
                return findInsertionPointAfterLast(
                        SemanticView.ViewType.PLUGINS,
                        SemanticView.ViewType.PROPERTY,
                        SemanticView.ViewType.REPOSITORIES
                ).orElse(originalSource.length());

            default:
                // Unknown blocks go at the end
                return originalSource.length();
        }
    }

    /**
     * Find insertion point after a specific block type.
     */
    private Optional<Integer> findInsertionPointAfter(SemanticView.ViewType type) {
        return findOriginalView(type)
                .filter(v -> v.getIRNode() != null)
                .map(v -> v.getIRNode().getEndOffset());
    }

    /**
     * Find insertion point after the last of several block types.
     */
    private Optional<Integer> findInsertionPointAfterLast(SemanticView.ViewType... types) {
        int maxEnd = -1;
        for (SemanticView.ViewType type : types) {
            Optional<Integer> end = findInsertionPointAfter(type);
            if (end.isPresent() && end.get() > maxEnd) {
                maxEnd = end.get();
            }
        }
        // Also check all properties (there may be multiple)
        for (SemanticView view : originalViews) {
            if (view.getType() == SemanticView.ViewType.PROPERTY && view.getIRNode() != null) {
                int end = view.getIRNode().getEndOffset();
                if (end > maxEnd) {
                    maxEnd = end;
                }
            }
        }
        return maxEnd >= 0 ? Optional.of(maxEnd) : Optional.empty();
    }

    /**
     * Detect indentation used in the source.
     */
    private static String detectIndentation(String source) {
        String[] lines = source.split("\n");
        for (String line : lines) {
            if (line.startsWith("    ")) {
                return "    ";
            }
            if (line.startsWith("\t")) {
                return "\t";
            }
        }
        return "    "; // Default to 4 spaces
    }

    /**
     * Get indentation for content inside a block (one level deeper).
     */
    public String getBlockIndentation() {
        return indentation;
    }

    /**
     * Format content with proper indentation for insertion inside a block.
     */
    public String indentContent(String content) {
        StringBuilder sb = new StringBuilder();
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append("\n");
            if (!lines[i].trim().isEmpty()) {
                sb.append(indentation).append(lines[i].trim());
            }
        }
        return sb.toString();
    }
}
