package com.github.gradleinserter.parser;

import com.github.gradleinserter.ir.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroovyScriptParserTest {

    private GroovyScriptParser parser;

    @BeforeEach
    void setUp() {
        parser = new GroovyScriptParser();
    }

    @Test
    @DisplayName("Should parse simple dependencies block")
    void parseSimpleDependencies() {
        String source = "dependencies {\n" +
                "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                "}\n";

        List<IRNode> nodes = parser.parse(source);

        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0)).isInstanceOf(BlockNode.class);

        BlockNode block = (BlockNode) nodes.get(0);
        assertThat(block.getName()).isEqualTo("dependencies");
        assertThat(block.getChildren()).hasSize(1);
        assertThat(block.getChildren().get(0)).isInstanceOf(MethodCallNode.class);

        MethodCallNode methodCall = (MethodCallNode) block.getChildren().get(0);
        assertThat(methodCall.getMethodName()).isEqualTo("implementation");
        assertThat(methodCall.getFirstArgument()).isEqualTo("com.google.guava:guava:31.0-jre");
    }

    @Test
    @DisplayName("Should parse plugins block")
    void parsePluginsBlock() {
        String source = "plugins {\n" +
                "    id 'java-library'\n" +
                "    id 'org.springframework.boot' version '3.0.0'\n" +
                "}\n";

        List<IRNode> nodes = parser.parse(source);

        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0)).isInstanceOf(BlockNode.class);

        BlockNode block = (BlockNode) nodes.get(0);
        assertThat(block.getName()).isEqualTo("plugins");
        assertThat(block.getChildren()).hasSize(2);
    }

    @Test
    @DisplayName("Should parse multiple top-level blocks")
    void parseMultipleBlocks() {
        String source = "plugins {\n" +
                "    id 'java-library'\n" +
                "}\n" +
                "\n" +
                "repositories {\n" +
                "    mavenCentral()\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                "}\n";

        List<IRNode> nodes = parser.parse(source);

        assertThat(nodes).hasSize(3);
        assertThat(nodes.get(0)).isInstanceOf(BlockNode.class);
        assertThat(nodes.get(1)).isInstanceOf(BlockNode.class);
        assertThat(nodes.get(2)).isInstanceOf(BlockNode.class);

        assertThat(((BlockNode) nodes.get(0)).getName()).isEqualTo("plugins");
        assertThat(((BlockNode) nodes.get(1)).getName()).isEqualTo("repositories");
        assertThat(((BlockNode) nodes.get(2)).getName()).isEqualTo("dependencies");
    }

    @Test
    @DisplayName("Should parse property assignments")
    void parsePropertyAssignment() {
        String source = "version = '1.0.0'\n" +
                "group = 'com.example'\n";

        List<IRNode> nodes = parser.parse(source);

        assertThat(nodes).hasSize(2);
        assertThat(nodes.get(0)).isInstanceOf(PropertyNode.class);
        assertThat(nodes.get(1)).isInstanceOf(PropertyNode.class);

        PropertyNode version = (PropertyNode) nodes.get(0);
        assertThat(version.getName()).isEqualTo("version");
        assertThat(version.getValue()).contains("1.0.0");

        PropertyNode group = (PropertyNode) nodes.get(1);
        assertThat(group.getName()).isEqualTo("group");
        assertThat(group.getValue()).contains("com.example");
    }

    @Test
    @DisplayName("Should preserve source offsets")
    void preserveSourceOffsets() {
        String source = "dependencies {\n    implementation 'test:test:1.0'\n}";

        List<IRNode> nodes = parser.parse(source);

        assertThat(nodes).hasSize(1);
        BlockNode block = (BlockNode) nodes.get(0);

        assertThat(block.getStartOffset()).isEqualTo(0);
        assertThat(block.getEndOffset()).isEqualTo(source.length());
        assertThat(block.getSourceText()).isEqualTo(source);
    }

    @Test
    @DisplayName("Should handle empty source")
    void handleEmptySource() {
        List<IRNode> nodes = parser.parse("");
        assertThat(nodes).isEmpty();

        nodes = parser.parse(null);
        assertThat(nodes).isEmpty();
    }

    @Test
    @DisplayName("Should handle method calls without closure")
    void methodCallWithoutClosure() {
        String source = "apply plugin: 'java'\n";

        List<IRNode> nodes = parser.parse(source);

        assertThat(nodes).isNotEmpty();
    }

    @Test
    @DisplayName("Should parse nested blocks")
    void parseNestedBlocks() {
        String source = "subprojects {\n" +
                "    dependencies {\n" +
                "        implementation 'common:lib:1.0'\n" +
                "    }\n" +
                "}\n";

        List<IRNode> nodes = parser.parse(source);

        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0)).isInstanceOf(BlockNode.class);

        BlockNode subprojects = (BlockNode) nodes.get(0);
        assertThat(subprojects.getName()).isEqualTo("subprojects");
        assertThat(subprojects.getChildren()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle various dependency notations")
    void variousDependencyNotations() {
        String source = "dependencies {\n" +
                "    implementation 'group:name:version'\n" +
                "    implementation group: 'g', name: 'n', version: 'v'\n" +
                "    implementation project(':module')\n" +
                "}\n";

        List<IRNode> nodes = parser.parse(source);

        assertThat(nodes).hasSize(1);
        BlockNode block = (BlockNode) nodes.get(0);
        assertThat(block.getChildren()).hasSize(3);
    }
}
