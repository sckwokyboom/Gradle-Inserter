package com.github.gradleinserter.merge;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.ir.IRNode;
import com.github.gradleinserter.ir.MethodCallNode;
import com.github.gradleinserter.view.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MergeStrategyTest {

    @Nested
    @DisplayName("DependenciesMergeStrategy")
    class DependenciesMergeStrategyTests {

        private DependenciesMergeStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new DependenciesMergeStrategy();
        }

        @Test
        @DisplayName("Should handle view type correctly")
        void viewType() {
            assertThat(strategy.getViewType()).isEqualTo(SemanticView.ViewType.DEPENDENCIES);
        }

        @Test
        @DisplayName("Should generate edit for new dependency")
        void newDependencyEdit() {
            String source = "dependencies {\n    implementation 'existing:dep:1.0'\n}";

            BlockNode originalBlock = createDependenciesBlock(source, 0, source.length(),
                    14, source.length() - 1,
                    List.of(createMethodCall("implementation", "existing:dep:1.0", 18, 55)));

            DependenciesView original = new DependenciesView(originalBlock);
            DependenciesView snippet = new DependenciesView(
                    List.of(createMethodCall("implementation", "new:dep:2.0", 0, 30)));

            MergeContext context = new MergeContext(source, List.of(original));

            List<IInsertionEdit> edits = strategy.merge(original, snippet, source, context);

            assertThat(edits).hasSize(1);
            assertThat(edits.get(0).getText()).contains("new:dep:2.0");
        }

        @Test
        @DisplayName("Should generate edit for version update")
        void versionUpdateEdit() {
            String source = "dependencies {\n    implementation 'group:name:1.0'\n}";
            int depStart = source.indexOf("implementation");
            int depEnd = source.indexOf("'\n}") + 1;

            MethodCallNode existingCall = createMethodCall("implementation", "group:name:1.0",
                    depStart, depEnd);
            existingCall = new MethodCallNode(
                    "implementation",
                    List.of("group:name:1.0"),
                    null,
                    depStart, depEnd,
                    "implementation 'group:name:1.0'"
            );

            BlockNode originalBlock = createDependenciesBlock(source, 0, source.length(),
                    14, source.length() - 1, List.of(existingCall));

            DependenciesView original = new DependenciesView(originalBlock);
            DependenciesView snippet = new DependenciesView(
                    List.of(createMethodCall("implementation", "group:name:2.0", 0, 30)));

            MergeContext context = new MergeContext(source, List.of(original));

            List<IInsertionEdit> edits = strategy.merge(original, snippet, source, context);

            assertThat(edits).hasSize(1);
            assertThat(edits.get(0).getText()).isEqualTo("2.0");
        }

        @Test
        @DisplayName("Should create new block when original is null")
        void createNewBlock() {
            String source = "plugins { id 'java' }";

            DependenciesView snippet = new DependenciesView(
                    List.of(createMethodCall("implementation", "new:dep:1.0", 0, 30)));

            MergeContext context = new MergeContext(source, Collections.emptyList());

            List<IInsertionEdit> edits = strategy.merge(null, snippet, source, context);

            assertThat(edits).hasSize(1);
            assertThat(edits.get(0).getText()).contains("dependencies {");
            assertThat(edits.get(0).getText()).contains("new:dep:1.0");
        }
    }

    @Nested
    @DisplayName("PluginsMergeStrategy")
    class PluginsMergeStrategyTests {

        private PluginsMergeStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new PluginsMergeStrategy();
        }

        @Test
        @DisplayName("Should handle view type correctly")
        void viewType() {
            assertThat(strategy.getViewType()).isEqualTo(SemanticView.ViewType.PLUGINS);
        }

        @Test
        @DisplayName("Should generate edit for new plugin")
        void newPluginEdit() {
            String source = "plugins {\n    id 'java'\n}";

            BlockNode originalBlock = createPluginsBlock(source, 0, source.length(),
                    9, source.length() - 1,
                    List.of(createMethodCall("id", "java", 14, 23)));

            PluginsView original = new PluginsView(originalBlock);
            PluginsView snippet = new PluginsView(
                    List.of(createMethodCall("id", "application", 0, 20)));

            MergeContext context = new MergeContext(source, List.of(original));

            List<IInsertionEdit> edits = strategy.merge(original, snippet, source, context);

            assertThat(edits).hasSize(1);
            assertThat(edits.get(0).getText()).contains("application");
        }
    }

    @Nested
    @DisplayName("MergeStrategyRegistry")
    class MergeStrategyRegistryTests {

        @Test
        @DisplayName("Should have default strategies registered")
        void defaultStrategies() {
            MergeStrategyRegistry registry = new MergeStrategyRegistry();

            assertThat(registry.getStrategy(SemanticView.ViewType.DEPENDENCIES)).isPresent();
            assertThat(registry.getStrategy(SemanticView.ViewType.PLUGINS)).isPresent();
            assertThat(registry.getStrategy(SemanticView.ViewType.REPOSITORIES)).isPresent();
            assertThat(registry.getStrategy(SemanticView.ViewType.UNKNOWN_BLOCK)).isPresent();
            assertThat(registry.getStrategy(SemanticView.ViewType.RAW)).isPresent();
        }

        @Test
        @DisplayName("Should allow registering custom strategy")
        void customStrategy() {
            MergeStrategyRegistry registry = new MergeStrategyRegistry();

            MergeStrategy<DependenciesView> customStrategy = new DependenciesMergeStrategy() {
                @Override
                public List<IInsertionEdit> merge(DependenciesView original, DependenciesView snippet,
                                                  String originalSource, MergeContext context) {
                    return Collections.emptyList();
                }
            };

            registry.register(customStrategy);

            var strategy = registry.getStrategy(SemanticView.ViewType.DEPENDENCIES);
            assertThat(strategy).isPresent();
            assertThat(strategy.get()).isSameAs(customStrategy);
        }
    }

    // Helper methods

    private MethodCallNode createMethodCall(String methodName, String arg, int start, int end) {
        return new MethodCallNode(
                methodName,
                List.of(arg),
                null,
                start, end,
                methodName + " '" + arg + "'"
        );
    }

    private BlockNode createDependenciesBlock(String source, int start, int end,
                                               int bodyStart, int bodyEnd,
                                               List<IRNode> children) {
        return new BlockNode("dependencies", start, end, bodyStart, bodyEnd, children, source);
    }

    private BlockNode createPluginsBlock(String source, int start, int end,
                                          int bodyStart, int bodyEnd,
                                          List<IRNode> children) {
        return new BlockNode("plugins", start, end, bodyStart, bodyEnd, children, source);
    }
}
