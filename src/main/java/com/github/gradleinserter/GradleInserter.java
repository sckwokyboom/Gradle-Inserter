package com.github.gradleinserter;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.ir.IRNode;
import com.github.gradleinserter.ir.MethodCallNode;
import com.github.gradleinserter.merge.*;
import com.github.gradleinserter.parser.GroovyScriptParser;
import com.github.gradleinserter.parser.ScriptParser;
import com.github.gradleinserter.view.*;

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

    private final ScriptParser parser;
    private final ViewExtractor viewExtractor;
    private final MergeStrategyRegistry strategyRegistry;
    private final SnippetAnalyzer snippetAnalyzer;

    private GradleInserter(ScriptParser parser, ViewExtractor viewExtractor,
                          MergeStrategyRegistry strategyRegistry) {
        this.parser = parser;
        this.viewExtractor = viewExtractor;
        this.strategyRegistry = strategyRegistry;
        this.snippetAnalyzer = new SnippetAnalyzer(parser, viewExtractor);
    }

    /**
     * Create a new GradleInserter with default configuration.
     */
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
    public List<IInsertionEdit> generateEdits(String originalScript, String snippet) {
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
        allEdits.sort(Comparator.comparingInt(IInsertionEdit::getStartOffset).reversed());

        return allEdits;
    }

    /**
     * Apply edits to the original script.
     *
     * @param originalScript the original script
     * @param edits          the edits to apply (will be sorted internally)
     * @return the modified script
     */
    public String applyEdits(String originalScript, List<IInsertionEdit> edits) {
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
    public String insert(String originalScript, String snippet) {
        List<IInsertionEdit> edits = generateEdits(originalScript, snippet);
        return applyEdits(originalScript, edits);
    }

    private SemanticView findMatchingOriginalView(List<SemanticView> originalViews,
                                                   SemanticView snippetView) {
        for (SemanticView original : originalViews) {
            if (viewsMatch(original, snippetView)) {
                return original;
            }
        }
        return null;
    }

    private boolean viewsMatch(SemanticView original, SemanticView snippet) {
        if (original.getType() != snippet.getType()) {
            return false;
        }

        // For unknown blocks, also check the block name
        if (original instanceof UnknownBlockView && snippet instanceof UnknownBlockView) {
            return ((UnknownBlockView) original).getBlockName()
                    .equals(((UnknownBlockView) snippet).getBlockName());
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private List<IInsertionEdit> mergeView(SemanticView original, SemanticView snippet,
                                            String originalScript, MergeContext context) {
        Optional<MergeStrategy<SemanticView>> strategy =
                strategyRegistry.findStrategy(original, snippet);

        if (strategy.isPresent()) {
            return strategy.get().merge(original, snippet, originalScript, context);
        }

        return Collections.emptyList();
    }

    /**
     * Builder for GradleInserter with custom configuration.
     */
    public static final class Builder {
        private ScriptParser parser;
        private ViewExtractor viewExtractor;
        private MergeStrategyRegistry strategyRegistry;

        private Builder() {
            this.parser = new GroovyScriptParser();
            this.viewExtractor = new ViewExtractor();
            this.strategyRegistry = new MergeStrategyRegistry();
        }

        public Builder withParser(ScriptParser parser) {
            this.parser = parser;
            return this;
        }

        public Builder withViewExtractor(ViewExtractor viewExtractor) {
            this.viewExtractor = viewExtractor;
            return this;
        }

        public Builder withStrategyRegistry(MergeStrategyRegistry registry) {
            this.strategyRegistry = registry;
            return this;
        }

        public Builder withStrategy(MergeStrategy<?> strategy) {
            this.strategyRegistry.register(strategy);
            return this;
        }

        public GradleInserter build() {
            return new GradleInserter(parser, viewExtractor, strategyRegistry);
        }
    }
}
