package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.MethodCallNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyItemTest {

    @Test
    @DisplayName("Should parse standard dependency notation")
    void parseStandardNotation() {
        MethodCallNode node = createMethodCall("implementation",
                "com.google.guava:guava:31.0-jre");

        DependencyItem item = DependencyItem.fromMethodCall(node);

        assertThat(item.getConfiguration()).isEqualTo("implementation");
        assertThat(item.getGroup()).isEqualTo("com.google.guava");
        assertThat(item.getName()).isEqualTo("guava");
        assertThat(item.getVersion()).isEqualTo("31.0-jre");
        assertThat(item.getCoordinateKey()).isEqualTo("com.google.guava:guava");
        assertThat(item.getFullCoordinate()).isEqualTo("com.google.guava:guava:31.0-jre");
    }

    @Test
    @DisplayName("Should parse dependency without version")
    void parseWithoutVersion() {
        MethodCallNode node = createMethodCall("implementation",
                "com.google.guava:guava");

        DependencyItem item = DependencyItem.fromMethodCall(node);

        assertThat(item.getGroup()).isEqualTo("com.google.guava");
        assertThat(item.getName()).isEqualTo("guava");
        assertThat(item.getVersion()).isNull();
        assertThat(item.getFullCoordinate()).isEqualTo("com.google.guava:guava");
    }

    @Test
    @DisplayName("Should parse dependency with classifier")
    void parseWithClassifier() {
        MethodCallNode node = createMethodCall("implementation",
                "org.lwjgl:lwjgl:3.3.1@natives-linux");

        DependencyItem item = DependencyItem.fromMethodCall(node);

        assertThat(item.getGroup()).isEqualTo("org.lwjgl");
        assertThat(item.getName()).isEqualTo("lwjgl");
        assertThat(item.getVersion()).isEqualTo("3.3.1");
        assertThat(item.getClassifier()).isEqualTo("natives-linux");
    }

    @Test
    @DisplayName("Should detect same artifact")
    void sameArtifact() {
        MethodCallNode node1 = createMethodCall("implementation",
                "com.google.guava:guava:30.0-jre");
        MethodCallNode node2 = createMethodCall("implementation",
                "com.google.guava:guava:31.0-jre");

        DependencyItem item1 = DependencyItem.fromMethodCall(node1);
        DependencyItem item2 = DependencyItem.fromMethodCall(node2);

        assertThat(item1.sameArtifact(item2)).isTrue();
    }

    @Test
    @DisplayName("Should detect different artifacts")
    void differentArtifacts() {
        MethodCallNode node1 = createMethodCall("implementation",
                "com.google.guava:guava:31.0-jre");
        MethodCallNode node2 = createMethodCall("implementation",
                "org.apache.commons:commons-lang3:3.12.0");

        DependencyItem item1 = DependencyItem.fromMethodCall(node1);
        DependencyItem item2 = DependencyItem.fromMethodCall(node2);

        assertThat(item1.sameArtifact(item2)).isFalse();
    }

    @Test
    @DisplayName("Should handle various configurations")
    void variousConfigurations() {
        String[] configs = {
                "implementation", "api", "compileOnly", "runtimeOnly",
                "testImplementation", "testCompileOnly", "annotationProcessor"
        };

        for (String config : configs) {
            MethodCallNode node = createMethodCall(config, "test:test:1.0");
            DependencyItem item = DependencyItem.fromMethodCall(node);
            assertThat(item.getConfiguration()).isEqualTo(config);
        }
    }

    @Test
    @DisplayName("Should create item with new version")
    void withVersion() {
        MethodCallNode node = createMethodCall("implementation",
                "com.google.guava:guava:30.0-jre");

        DependencyItem item = DependencyItem.fromMethodCall(node);
        DependencyItem updated = item.withVersion("31.0-jre");

        assertThat(updated.getVersion()).isEqualTo("31.0-jre");
        assertThat(updated.getGroup()).isEqualTo(item.getGroup());
        assertThat(updated.getName()).isEqualTo(item.getName());
        assertThat(updated.getConfiguration()).isEqualTo(item.getConfiguration());
    }

    @Test
    @DisplayName("Should parse dependency with simple GString version")
    void parseWithGStringVersion() {
        MethodCallNode node = createMethodCall("implementation",
                "com.example:library:${version}");

        DependencyItem item = DependencyItem.fromMethodCall(node);

        assertThat(item.getGroup()).isEqualTo("com.example");
        assertThat(item.getName()).isEqualTo("library");
        assertThat(item.getVersion()).isEqualTo("${version}");
    }

    @Test
    @DisplayName("Should parse dependency with nested GString version")
    void parseWithNestedGStringVersion() {
        MethodCallNode node = createMethodCall("implementation",
                "com.example:library:${{{version}}}");

        DependencyItem item = DependencyItem.fromMethodCall(node);

        assertThat(item.getGroup()).isEqualTo("com.example");
        assertThat(item.getName()).isEqualTo("library");
        assertThat(item.getVersion()).isEqualTo("${{{version}}}");
    }

    @Test
    @DisplayName("Should parse dependency with complex property reference")
    void parseWithComplexPropertyReference() {
        MethodCallNode node = createMethodCall("implementation",
                "com.example:library:${props['version.key']}");

        DependencyItem item = DependencyItem.fromMethodCall(node);

        assertThat(item.getGroup()).isEqualTo("com.example");
        assertThat(item.getName()).isEqualTo("library");
        assertThat(item.getVersion()).isEqualTo("${props['version.key']}");
    }

    @Test
    @DisplayName("Should parse dependency with multiple colons in version")
    void parseWithMultipleColonsInVersion() {
        MethodCallNode node = createMethodCall("implementation",
                "com.example:library:${props['group:name:version']}");

        DependencyItem item = DependencyItem.fromMethodCall(node);

        assertThat(item.getGroup()).isEqualTo("com.example");
        assertThat(item.getName()).isEqualTo("library");
        assertThat(item.getVersion()).isEqualTo("${props['group:name:version']}");
    }

    @Test
    @DisplayName("Should parse dependency with exclude block")
    void parseWithExcludeBlock() {
        String closureSource = "{\n    exclude group: 'org.apache', module: 'commons-io'\n}";
        MethodCallNode node = createMethodCallWithClosure(
                "implementation",
                "com.example:library:1.0.0",
                closureSource
        );

        DependencyItem item = DependencyItem.fromMethodCall(node);

        assertThat(item.getGroup()).isEqualTo("com.example");
        assertThat(item.getName()).isEqualTo("library");
        assertThat(item.getVersion()).isEqualTo("1.0.0");
        assertThat(item.hasExcludes()).isTrue();
        assertThat(item.getExcludes()).hasSize(1);

        ExcludeItem exclude = item.getExcludes().get(0);
        assertThat(exclude.getGroup()).isEqualTo("org.apache");
        assertThat(exclude.getModule()).isEqualTo("commons-io");
    }

    @Test
    @DisplayName("Should parse dependency with multiple exclude blocks")
    void parseWithMultipleExcludeBlocks() {
        String closureSource = "{\n" +
                "    exclude group: 'org.apache', module: 'commons-io'\n" +
                "    exclude group: 'com.google.guava', module: 'guava'\n" +
                "}";
        MethodCallNode node = createMethodCallWithClosure(
                "implementation",
                "com.example:library:1.0.0",
                closureSource
        );

        DependencyItem item = DependencyItem.fromMethodCall(node);

        assertThat(item.hasExcludes()).isTrue();
        assertThat(item.getExcludes()).hasSize(2);

        ExcludeItem exclude1 = item.getExcludes().get(0);
        assertThat(exclude1.getGroup()).isEqualTo("org.apache");
        assertThat(exclude1.getModule()).isEqualTo("commons-io");

        ExcludeItem exclude2 = item.getExcludes().get(1);
        assertThat(exclude2.getGroup()).isEqualTo("com.google.guava");
        assertThat(exclude2.getModule()).isEqualTo("guava");
    }

    @Test
    @DisplayName("Should parse dependency with GString in exclude block")
    void parseWithGStringInExcludeBlock() {
        String closureSource = "{\n    exclude group: ${excludeGroup}, module: ${excludeModule}\n}";
        MethodCallNode node = createMethodCallWithClosure(
                "implementation",
                "com.example:library:1.0.0",
                closureSource
        );

        DependencyItem item = DependencyItem.fromMethodCall(node);

        assertThat(item.hasExcludes()).isTrue();
        assertThat(item.getExcludes()).hasSize(1);

        // GString expressions in excludes are complex and would be evaluated at runtime in Gradle
        // We just verify the exclude statement is detected
    }

    private MethodCallNode createMethodCall(String methodName, String argument) {
        return new MethodCallNode(
                methodName,
                List.of(argument),
                null,
                0, 50,
                methodName + " '" + argument + "'"
        );
    }

    private MethodCallNode createMethodCallWithClosure(String methodName, String argument, String closureSource) {
        com.github.gradleinserter.ir.RawNode closureBody =
                new com.github.gradleinserter.ir.RawNode(0, closureSource.length(), closureSource);

        return new MethodCallNode(
                methodName,
                List.of(argument),
                closureBody,
                0, 50 + closureSource.length(),
                methodName + " '" + argument + "' " + closureSource
        );
    }
}
