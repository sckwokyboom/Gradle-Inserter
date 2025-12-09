package com.github.gradleinserter;

import com.github.gradleinserter.parser.GroovyScriptParser;
import com.github.gradleinserter.view.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SnippetAnalyzerTest {

    private SnippetAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SnippetAnalyzer(new GroovyScriptParser(), new ViewExtractor());
    }

    @Test
    @DisplayName("Should analyze complete dependencies block")
    void completeBlock() {
        String snippet = "dependencies {\n" +
                "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                "}\n";

        List<SemanticView> views = analyzer.analyze(snippet);

        assertThat(views).hasSize(1);
        assertThat(views.get(0)).isInstanceOf(DependenciesView.class);

        DependenciesView depsView = (DependenciesView) views.get(0);
        assertThat(depsView.getDependencies()).hasSize(1);
    }

    @Test
    @DisplayName("Should analyze unwrapped dependencies")
    void unwrappedDependencies() {
        String snippet = "implementation 'com.google.guava:guava:31.0-jre'\n" +
                "testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'\n";

        List<SemanticView> views = analyzer.analyze(snippet);

        assertThat(views).hasSize(1);
        assertThat(views.get(0)).isInstanceOf(DependenciesView.class);

        DependenciesView depsView = (DependenciesView) views.get(0);
        assertThat(depsView.getDependencies()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle markdown code blocks")
    void markdownCodeBlock() {
        String snippet = "```groovy\n" +
                "dependencies {\n" +
                "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                "}\n" +
                "```\n";

        List<SemanticView> views = analyzer.analyze(snippet);

        assertThat(views).hasSize(1);
        assertThat(views.get(0)).isInstanceOf(DependenciesView.class);
    }

    @Test
    @DisplayName("Should handle gradle code block marker")
    void gradleCodeBlock() {
        String snippet = "```gradle\n" +
                "dependencies {\n" +
                "    implementation 'new:dep:1.0'\n" +
                "}\n" +
                "```\n";

        List<SemanticView> views = analyzer.analyze(snippet);

        assertThat(views).hasSize(1);
    }

    @Test
    @DisplayName("Should handle multiple blocks")
    void multipleBlocks() {
        String snippet = "plugins {\n" +
                "    id 'java'\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    implementation 'dep:dep:1.0'\n" +
                "}\n";

        List<SemanticView> views = analyzer.analyze(snippet);

        assertThat(views).hasSize(2);

        boolean hasPlugins = views.stream().anyMatch(v -> v instanceof PluginsView);
        boolean hasDeps = views.stream().anyMatch(v -> v instanceof DependenciesView);

        assertThat(hasPlugins).isTrue();
        assertThat(hasDeps).isTrue();
    }

    @Test
    @DisplayName("Should handle empty snippet")
    void emptySnippet() {
        List<SemanticView> views = analyzer.analyze("");
        assertThat(views).isEmpty();

        views = analyzer.analyze(null);
        assertThat(views).isEmpty();

        views = analyzer.analyze("   \n\t  ");
        assertThat(views).isEmpty();
    }

    @Test
    @DisplayName("Should handle mixed wrapped and unwrapped")
    void mixedContent() {
        String snippet = "plugins {\n" +
                "    id 'java'\n" +
                "}\n" +
                "\n" +
                "implementation 'standalone:dep:1.0'\n";

        List<SemanticView> views = analyzer.analyze(snippet);

        // Should detect both plugins block and unwrapped dependency
        assertThat(views).isNotEmpty();
    }

    @Test
    @DisplayName("Should fallback to raw view for unparseable content")
    void unparsableContent() {
        String snippet = "some random unparseable content { }}}";

        List<SemanticView> views = analyzer.analyze(snippet);

        // Should not throw, should return some view (possibly raw)
        assertThat(views).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle single line dependency")
    void singleLineDependency() {
        String snippet = "implementation 'single:line:dep'";

        List<SemanticView> views = analyzer.analyze(snippet);

        assertThat(views).isNotEmpty();
    }
}
