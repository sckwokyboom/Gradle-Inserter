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
