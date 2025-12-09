package com.github.gradleinserter;

import com.github.gradleinserter.api.IInsertionEdit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GradleInserterTest {

    private GradleInserter inserter;

    @BeforeEach
    void setUp() {
        inserter = GradleInserter.create();
    }

    @Nested
    @DisplayName("Dependencies")
    class DependenciesTests {

        @Test
        @DisplayName("Should add new dependency to existing block")
        void addNewDependency() {
            String original = "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet = "dependencies {\n" +
                    "    implementation 'org.apache.commons:commons-lang3:3.12.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("com.google.guava:guava:30.0-jre");
            assertThat(result).contains("org.apache.commons:commons-lang3:3.12.0");
        }

        @Test
        @DisplayName("Should update existing dependency version")
        void updateDependencyVersion() {
            String original = "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet = "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.1-jre'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("31.1-jre");
            assertThat(result).doesNotContain("30.0-jre");
        }

        @Test
        @DisplayName("Should handle multiple dependencies in snippet")
        void multipleNewDependencies() {
            String original = "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet = "dependencies {\n" +
                    "    implementation 'org.apache.commons:commons-lang3:3.12.0'\n" +
                    "    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("com.google.guava:guava:30.0-jre");
            assertThat(result).contains("org.apache.commons:commons-lang3:3.12.0");
            assertThat(result).contains("org.junit.jupiter:junit-jupiter:5.10.0");
        }

        @Test
        @DisplayName("Should create dependencies block if it doesn't exist")
        void createDependenciesBlock() {
            String original = "plugins {\n" +
                    "    id 'java-library'\n" +
                    "}\n";

            String snippet = "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("plugins");
            assertThat(result).contains("dependencies {");
            assertThat(result).contains("com.google.guava:guava:31.0-jre");
        }

        @Test
        @DisplayName("Should handle unwrapped dependency declarations")
        void unwrappedDependencies() {
            String original = "dependencies {\n" +
                    "    implementation 'existing:dep:1.0'\n" +
                    "}\n";

            String snippet = "implementation 'new:dep:2.0'\n" +
                    "testImplementation 'test:dep:3.0'\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("existing:dep:1.0");
            assertThat(result).contains("new:dep:2.0");
            assertThat(result).contains("test:dep:3.0");
        }

        @Test
        @DisplayName("Should preserve original formatting")
        void preserveFormatting() {
            String original = "dependencies {\n" +
                    "    // Core dependencies\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "\n" +
                    "    // Test dependencies\n" +
                    "    testImplementation 'junit:junit:4.13'\n" +
                    "}\n";

            String snippet = "dependencies {\n" +
                    "    implementation 'org.apache.commons:commons-lang3:3.12.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            // Original comments should be preserved
            assertThat(result).contains("// Core dependencies");
            assertThat(result).contains("// Test dependencies");
            assertThat(result).contains("com.google.guava:guava:30.0-jre");
            assertThat(result).contains("org.apache.commons:commons-lang3:3.12.0");
        }
    }

    @Nested
    @DisplayName("Plugins")
    class PluginsTests {

        @Test
        @DisplayName("Should add new plugin to existing block")
        void addNewPlugin() {
            String original = "plugins {\n" +
                    "    id 'java-library'\n" +
                    "}\n";

            String snippet = "plugins {\n" +
                    "    id 'org.springframework.boot' version '3.0.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("java-library");
            assertThat(result).contains("org.springframework.boot");
            assertThat(result).contains("version '3.0.0'");
        }

        @Test
        @DisplayName("Should update plugin version")
        void updatePluginVersion() {
            String original = "plugins {\n" +
                    "    id 'org.springframework.boot' version '2.7.0'\n" +
                    "}\n";

            String snippet = "plugins {\n" +
                    "    id 'org.springframework.boot' version '3.1.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("version '3.1.0'");
            assertThat(result).doesNotContain("version '2.7.0'");
        }

        @Test
        @DisplayName("Should create plugins block if not exists")
        void createPluginsBlock() {
            String original = "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet = "plugins {\n" +
                    "    id 'java-library'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("plugins {");
            assertThat(result).contains("id 'java-library'");
            assertThat(result).contains("dependencies {");
        }
    }

    @Nested
    @DisplayName("Repositories")
    class RepositoriesTests {

        @Test
        @DisplayName("Should add new repository")
        void addNewRepository() {
            String original = "repositories {\n" +
                    "    mavenCentral()\n" +
                    "}\n";

            String snippet = "repositories {\n" +
                    "    google()\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("mavenCentral()");
            assertThat(result).contains("google()");
        }

        @Test
        @DisplayName("Should not duplicate existing repository")
        void noDuplicateRepository() {
            String original = "repositories {\n" +
                    "    mavenCentral()\n" +
                    "}\n";

            String snippet = "repositories {\n" +
                    "    mavenCentral()\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            // Should appear only once
            int count = result.split("mavenCentral").length - 1;
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Complex scenarios")
    class ComplexScenarios {

        @Test
        @DisplayName("Should handle multiple blocks in snippet")
        void multipleBlocks() {
            String original = "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n" +
                    "\n" +
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet = "plugins {\n" +
                    "    id 'application'\n" +
                    "}\n" +
                    "\n" +
                    "dependencies {\n" +
                    "    implementation 'org.apache.commons:commons-lang3:3.12.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            // Both original plugins should be preserved
            assertThat(result).contains("id 'java'");
            assertThat(result).contains("id 'application'");

            // Both original and new dependencies should be present
            assertThat(result).contains("guava:30.0-jre");
            assertThat(result).contains("commons-lang3:3.12.0");
        }

        @Test
        @DisplayName("Should handle snippet with markdown code block")
        void markdownCodeBlock() {
            String original = "dependencies {\n" +
                    "    implementation 'existing:dep:1.0'\n" +
                    "}\n";

            String snippet = "```groovy\n" +
                    "dependencies {\n" +
                    "    implementation 'new:dep:2.0'\n" +
                    "}\n" +
                    "```\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("existing:dep:1.0");
            assertThat(result).contains("new:dep:2.0");
            assertThat(result).doesNotContain("```");
        }

        @Test
        @DisplayName("Should handle empty snippet gracefully")
        void emptySnippet() {
            String original = "dependencies {\n" +
                    "    implementation 'existing:dep:1.0'\n" +
                    "}\n";

            String snippet = "";

            String result = inserter.insert(original, snippet);

            assertThat(result).isEqualTo(original);
        }

        @Test
        @DisplayName("Should handle empty original gracefully")
        void emptyOriginal() {
            String original = "";

            String snippet = "dependencies {\n" +
                    "    implementation 'new:dep:1.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("dependencies {");
            assertThat(result).contains("new:dep:1.0");
        }

        @Test
        @DisplayName("Should update multiple versions in single snippet")
        void updateMultipleVersions() {
            String original = "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "    implementation 'org.apache.commons:commons-lang3:3.10.0'\n" +
                    "    testImplementation 'org.junit.jupiter:junit-jupiter:5.8.0'\n" +
                    "}\n";

            String snippet = "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.1-jre'\n" +
                    "    implementation 'org.apache.commons:commons-lang3:3.12.0'\n" +
                    "    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);

            assertThat(result).contains("guava:31.1-jre");
            assertThat(result).contains("commons-lang3:3.12.0");
            assertThat(result).contains("junit-jupiter:5.10.0");

            assertThat(result).doesNotContain("guava:30.0-jre");
            assertThat(result).doesNotContain("commons-lang3:3.10.0");
            assertThat(result).doesNotContain("junit-jupiter:5.8.0");
        }
    }

    @Nested
    @DisplayName("Edit generation")
    class EditGenerationTests {

        @Test
        @DisplayName("Should generate correct edit offsets")
        void correctEditOffsets() {
            String original = "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet = "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                    "}\n";

            List<IInsertionEdit> edits = inserter.generateEdits(original, snippet);

            assertThat(edits).isNotEmpty();

            for (IInsertionEdit edit : edits) {
                assertThat(edit.getStartOffset()).isGreaterThanOrEqualTo(0);
                assertThat(edit.getEndOffset()).isLessThanOrEqualTo(original.length());
                assertThat(edit.getStartOffset()).isLessThanOrEqualTo(edit.getEndOffset());
            }
        }

        @Test
        @DisplayName("Edits should be sorted by offset descending")
        void editsSortedDescending() {
            String original = "dependencies {\n" +
                    "    implementation 'dep1:dep1:1.0'\n" +
                    "    implementation 'dep2:dep2:1.0'\n" +
                    "    implementation 'dep3:dep3:1.0'\n" +
                    "}\n";

            String snippet = "dependencies {\n" +
                    "    implementation 'dep1:dep1:2.0'\n" +
                    "    implementation 'dep2:dep2:2.0'\n" +
                    "    implementation 'dep3:dep3:2.0'\n" +
                    "}\n";

            List<IInsertionEdit> edits = inserter.generateEdits(original, snippet);

            for (int i = 0; i < edits.size() - 1; i++) {
                assertThat(edits.get(i).getStartOffset())
                        .isGreaterThanOrEqualTo(edits.get(i + 1).getStartOffset());
            }
        }
    }
}
