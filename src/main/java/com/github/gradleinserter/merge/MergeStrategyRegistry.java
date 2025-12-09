package com.github.gradleinserter.merge;

import com.github.gradleinserter.view.SemanticView;

import java.util.*;

/**
 * Registry for merge strategies.
 * Allows registration and lookup of strategies by view type.
 */
public class MergeStrategyRegistry {

    private final Map<SemanticView.ViewType, MergeStrategy<?>> strategies = new HashMap<>();

    public MergeStrategyRegistry() {
        // Register default strategies
        register(new DependenciesMergeStrategy());
        register(new PluginsMergeStrategy());
        register(new RepositoriesMergeStrategy());
        register(new UnknownBlockMergeStrategy());
        register(new RawContentMergeStrategy());
    }

    /**
     * Register a merge strategy.
     */
    public void register(MergeStrategy<?> strategy) {
        strategies.put(strategy.getViewType(), strategy);
    }

    /**
     * Get strategy for a view type.
     */
    @SuppressWarnings("unchecked")
    public <T extends SemanticView> Optional<MergeStrategy<T>> getStrategy(SemanticView.ViewType type) {
        MergeStrategy<?> strategy = strategies.get(type);
        if (strategy != null) {
            return Optional.of((MergeStrategy<T>) strategy);
        }
        return Optional.empty();
    }

    /**
     * Find a strategy that can handle the given views.
     */
    @SuppressWarnings("unchecked")
    public <T extends SemanticView> Optional<MergeStrategy<T>> findStrategy(
            SemanticView original, SemanticView snippet) {
        for (MergeStrategy<?> strategy : strategies.values()) {
            if (strategy.canHandle(original, snippet)) {
                return Optional.of((MergeStrategy<T>) strategy);
            }
        }
        return Optional.empty();
    }
}
