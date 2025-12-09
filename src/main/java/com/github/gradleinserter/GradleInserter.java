package com.github.gradleinserter;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.ir.IRNode;
import com.github.gradleinserter.ir.MethodCallNode;
import com.github.gradleinserter.merge.*;
import com.github.gradleinserter.parser.GroovyScriptParser;
import com.github.gradleinserter.parser.ScriptParser;
import com.github.gradleinserter.view.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Main public API for inserting Groovy snippets into build.gradle scripts.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * GradleInserter inserter = GradleInserter.create();
 * List<IInsertionEdit> edits = inserter.generateEdits(originalScript, snippet);
 * String result = inserter.applyEdits(originalScript, edits);
 * }</pre>
 */
public final class GradleInserter {

    @NotNull
    private final ScriptParser parser;
    @NotNull
    private final ViewExtractor viewExtractor;
    @NotNull
    private final MergeStrategyRegistry strategyRegistry;
    @NotNull
    private final SnippetAnalyzer snippetAnalyzer;

    private GradleInserter(@NotNull ScriptParser parser, @NotNull ViewExtractor viewExtractor,
                          @NotNull MergeStrategyRegistry strategyRegistry) {
        this.parser = parser;
        this.viewExtractor = viewExtractor;
        this.strategyRegistry = strategyRegistry;
        this.snippetAnalyzer = new SnippetAnalyzer(parser, viewExtractor);
    }

    /**
     * Create a new GradleInserter with default configuration.
     */
    @NotNull
    public static GradleInserter create() {
        return new GradleInserter(
                new GroovyScriptParser(),
                new ViewExtractor(),
                new MergeStrategyRegistry()
        );
    }

    /**
     * Create a builder for custom configuration.
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Generate edits to insert snippet into original script.
     *
     * @param originalScript the original build.gradle content
     * @param snippet        the snippet with LLM recommendations
     * @return list of edits to apply (sorted by offset, descending)
     */
    @NotNull
    public List<IInsertionEdit> generateEdits(@NotNull String originalScript, @Nullable String snippet) {
        // Parse original script
        List<IRNode> originalNodes = parser.parse(originalScript);
        List<SemanticView> originalViews = viewExtractor.extract(originalNodes);

        // Analyze and parse snippet (may contain partial/unwrapped content)
        List<SemanticView> snippetViews = snippetAnalyzer.analyze(snippet);

        // Create merge context
        MergeContext context = new MergeContext(originalScript, originalViews);

        // Generate edits for each snippet view
        List<IInsertionEdit> allEdits = new ArrayList<>();

        for (SemanticView snippetView : snippetViews) {
            SemanticView originalView = findMatchingOriginalView(originalViews, snippetView);
            List<IInsertionEdit> edits = mergeView(originalView, snippetView, originalScript, context);
            allEdits.addAll(edits);
        }

        // Sort edits by offset (descending) so they can be applied from end to start
        // When offsets are equal, maintain original order (stable sort with index)
        List<IndexedEdit> indexed = new ArrayList<>();
        for (int i = 0; i < allEdits.size(); i++) {
            indexed.add(new IndexedEdit(allEdits.get(i), i));
        }
        indexed.sort(Comparator
                .comparingInt((IndexedEdit e) -> e.edit.getStartOffset()).reversed()
                .thenComparingInt(e -> -e.index)); // Later additions first at same position

        allEdits.clear();
        for (IndexedEdit ie : indexed) {
            allEdits.add(ie.edit);
        }

        return allEdits;
    }

    /**
     * Apply edits to the original script.
     *
     * @param originalScript the original script
     * @param edits          the edits to apply (will be sorted internally)
     * @return the modified script
     */
    @NotNull
    public String applyEdits(@NotNull String originalScript, @NotNull List<IInsertionEdit> edits) {
        // Sort edits by offset (descending) to apply from end to start
        List<IInsertionEdit> sortedEdits = new ArrayList<>(edits);
        sortedEdits.sort(Comparator.comparingInt(IInsertionEdit::getStartOffset).reversed());

        StringBuilder result = new StringBuilder(originalScript);

        for (IInsertionEdit edit : sortedEdits) {
            result.replace(edit.getStartOffset(), edit.getEndOffset(), edit.getText());
        }

        return result.toString();
    }

    /**
     * Convenience method to directly get the modified script.
     */
    @NotNull
    public String insert(@NotNull String originalScript, @Nullable String snippet) {
        List<IInsertionEdit> edits = generateEdits(originalScript, snippet);
        return applyEdits(originalScript, edits);
    }

    @Nullable
    private SemanticView findMatchingOriginalView(@NotNull List<SemanticView> originalViews,
                                                   @NotNull SemanticView snippetView) {
        for (SemanticView original : originalViews) {
            if (viewsMatch(original, snippetView)) {
                return original;
            }
        }
        return null;
    }

    private boolean viewsMatch(@NotNull SemanticView original, @NotNull SemanticView snippet) {
        if (original.getType() != snippet.getType()) {
            return false;
        }

        // For unknown blocks, also check the block name
        if (original instanceof UnknownBlockView && snippet instanceof UnknownBlockView) {
            return ((UnknownBlockView) original).getBlockName()
                    .equals(((UnknownBlockView) snippet).getBlockName());
        }

        // For properties, match by property name
        if (original instanceof PropertyView && snippet instanceof PropertyView) {
            return ((PropertyView) original).getName()
                    .equals(((PropertyView) snippet).getName());
        }

        return true;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private List<IInsertionEdit> mergeView(@Nullable SemanticView original, @NotNull SemanticView snippet,
                                            @NotNull String originalScript, @NotNull MergeContext context) {
        Optional<MergeStrategy<SemanticView>> strategy =
                strategyRegistry.findStrategy(original, snippet);

        if (strategy.isPresent()) {
            return strategy.get().merge(original, snippet, originalScript, context);
        }

        return Collections.emptyList();
    }

    /**
     * Helper class for stable sorting of edits.
     */
    private static final class IndexedEdit {
        @NotNull
        final IInsertionEdit edit;
        final int index;

        IndexedEdit(@NotNull IInsertionEdit edit, int index) {
            this.edit = edit;
            this.index = index;
        }
    }

    /**
     * Builder for GradleInserter with custom configuration.
     */
    public static final class Builder {
        @NotNull
        private ScriptParser parser;
        @NotNull
        private ViewExtractor viewExtractor;
        @NotNull
        private MergeStrategyRegistry strategyRegistry;

        private Builder() {
            this.parser = new GroovyScriptParser();
            this.viewExtractor = new ViewExtractor();
            this.strategyRegistry = new MergeStrategyRegistry();
        }

        @NotNull
        public Builder withParser(@NotNull ScriptParser parser) {
            this.parser = parser;
            return this;
        }

        @NotNull
        public Builder withViewExtractor(@NotNull ViewExtractor viewExtractor) {
            this.viewExtractor = viewExtractor;
            return this;
        }

        @NotNull
        public Builder withStrategyRegistry(@NotNull MergeStrategyRegistry registry) {
            this.strategyRegistry = registry;
            return this;
        }

        @NotNull
        public Builder withStrategy(@NotNull MergeStrategy<?> strategy) {
            this.strategyRegistry.register(strategy);
            return this;
        }

        @NotNull
        public GradleInserter build() {
            return new GradleInserter(parser, viewExtractor, strategyRegistry);
        }
    }
}
