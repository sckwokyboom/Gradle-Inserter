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

    private MethodCallNode createMethodCall(String methodName, String argument) {
        return new MethodCallNode(
                methodName,
                List.of(argument),
                null,
                0, 50,
                methodName + " '" + argument + "'"
        );
    }
}
