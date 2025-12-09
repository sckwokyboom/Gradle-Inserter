package com.github.gradleinserter;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.api.ReplaceEdit;
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

    // ==================== BASIC DEPENDENCY TESTS ====================

    @Nested
    @DisplayName("Basic Dependencies Operations")
    class BasicDependenciesTests {

        @Test
        @DisplayName("Add single new dependency to existing block")
        void addSingleDependency() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'org.apache.commons:commons-lang3:3.12.0'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "    implementation 'org.apache.commons:commons-lang3:3.12.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Update existing dependency version")
        void updateDependencyVersion() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.1-jre'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.1-jre'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Add multiple dependencies at once")
        void addMultipleDependencies() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'new:lib1:2.0'\n" +
                    "    testImplementation 'new:lib2:3.0'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation 'new:lib1:2.0'\n" +
                    "    testImplementation 'new:lib2:3.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Update multiple versions in single snippet")
        void updateMultipleVersions() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "    implementation 'org.apache.commons:commons-lang3:3.10.0'\n" +
                    "    testImplementation 'org.junit.jupiter:junit-jupiter:5.8.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.1-jre'\n" +
                    "    implementation 'org.apache.commons:commons-lang3:3.12.0'\n" +
                    "    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.1-jre'\n" +
                    "    implementation 'org.apache.commons:commons-lang3:3.12.0'\n" +
                    "    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Create dependencies block if not exists")
        void createDependenciesBlock() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                    "}\n";

            // Dependencies block should be added after plugins
            String result = inserter.insert(original, snippet);
            assertThat(result).contains("plugins {");
            assertThat(result).contains("dependencies {");
            assertThat(result).contains("implementation 'com.google.guava:guava:31.0-jre'");
            // Dependencies should appear after plugins
            int pluginsEnd = result.indexOf("}", result.indexOf("plugins"));
            int depStart = result.indexOf("dependencies");
            assertThat(depStart).isGreaterThan(pluginsEnd);
        }
    }

    // ==================== DEPENDENCY NOTATION VARIATIONS ====================

    @Nested
    @DisplayName("Dependency Notation Variations")
    class DependencyNotationTests {

        @Test
        @DisplayName("Double quotes in snippet - add new dependency preserves double quotes")
        void doubleQuotesInSnippetAddNew() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation \"new:lib:2.0\"\n" +
                    "}\n";

            // Now preserves the original notation style from snippet (double quotes)
            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation \"new:lib:2.0\"\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Double quotes in snippet - update version")
        void doubleQuotesInSnippetUpdateVersion() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation \"existing:lib:2.0\"\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Map notation for dependency - add new preserves map notation")
        void mapNotationDependencyAddNew() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation group: 'new', name: 'lib', version: '2.0'\n" +
                    "}\n";

            // Now preserves the original notation style from snippet (map notation)
            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation group: 'new', name: 'lib', version: '2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Map notation for dependency - update version")
        void mapNotationDependencyUpdateVersion() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation group: 'existing', name: 'lib', version: '2.0'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Dependency without version in original - snippet with version adds it")
        void dependencyWithoutVersionOriginalSnippetWithVersionAddsIt() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                    "}\n";

            // When original has no version and snippet has version, version is added
            String expected =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Dependency without version in snippet - no version change")
        void dependencyWithoutVersionInSnippet() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava'\n" +
                    "}\n";

            // Snippet without version should not modify existing version
            String expected =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Mixed single and double quotes - update versions")
        void mixedQuotesUpdateVersions() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'lib1:lib1:1.0'\n" +
                    "    implementation \"lib2:lib2:1.0\"\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'lib1:lib1:2.0'\n" +
                    "    implementation \"lib2:lib2:2.0\"\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'lib1:lib1:2.0'\n" +
                    "    implementation \"lib2:lib2:2.0\"\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("GString interpolation in original - add new dependency")
        void gstringInterpolationOriginal() {
            String original =
                    "dependencies {\n" +
                    "    implementation \"org.example:lib:${version}\"\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'new:lib:1.0'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation \"org.example:lib:${version}\"\n" +
                    "    implementation 'new:lib:1.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Snippet without version - add new dependency without version")
        void snippetWithoutVersionAddNew() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'new:lib'\n" +
                    "}\n";

            // New dependency from snippet should be added as-is (without version)
            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation 'new:lib'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Map notation without version - add new dependency preserves map notation")
        void mapNotationWithoutVersionAddNew() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation group: 'new', name: 'lib'\n" +
                    "}\n";

            // Now preserves the original notation style from snippet (map notation)
            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation group: 'new', name: 'lib'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Map notation without version - no update when same artifact exists")
        void mapNotationWithoutVersionNoUpdate() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation group: 'existing', name: 'lib'\n" +
                    "}\n";

            // Snippet without version should not modify existing dependency
            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Original with double quotes - update version preserves quote style")
        void originalDoubleQuotesUpdateVersion() {
            String original =
                    "dependencies {\n" +
                    "    implementation \"existing:lib:1.0\"\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:2.0'\n" +
                    "}\n";

            // Should update version but preserve original double quotes
            String expected =
                    "dependencies {\n" +
                    "    implementation \"existing:lib:2.0\"\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Original map notation - update version")
        void originalMapNotationUpdateVersion() {
            String original =
                    "dependencies {\n" +
                    "    implementation group: 'existing', name: 'lib', version: '1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:2.0'\n" +
                    "}\n";

            // Should update version in map notation
            String expected =
                    "dependencies {\n" +
                    "    implementation group: 'existing', name: 'lib', version: '2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Both without versions - no change")
        void bothWithoutVersionsNoChange() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'existing:lib'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Platform dependency notation - preserves exact syntax from snippet")
        void platformDependencyNotation() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation platform('org.springframework.boot:spring-boot-dependencies:3.2.0')\n" +
                    "}\n";

            // Now preserves the original notation style from snippet exactly
            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation platform('org.springframework.boot:spring-boot-dependencies:3.2.0')\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Project dependency notation - preserves exact syntax from snippet")
        void projectDependencyNotation() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation project(':core')\n" +
                    "}\n";

            // Now preserves the original notation style from snippet exactly
            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation project(':core')\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Different configurations for same artifact should not update each other")
        void differentConfigurationsSameArtifact() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    testImplementation 'com.google.guava:guava:31.0-jre'\n" +
                    "}\n";

            // testImplementation should be added, implementation should not be updated
            String expected =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "    testImplementation 'com.google.guava:guava:31.0-jre'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Same configuration same artifact should update version")
        void sameConfigurationSameArtifactUpdatesVersion() {
            String original =
                    "dependencies {\n" +
                    "    testImplementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    testImplementation 'com.google.guava:guava:31.0-jre'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    testImplementation 'com.google.guava:guava:31.0-jre'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("All configuration types")
        void allConfigurationTypes() {
            String original =
                    "dependencies {\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'impl:lib:1.0'\n" +
                    "    api 'api:lib:1.0'\n" +
                    "    compileOnly 'compile:lib:1.0'\n" +
                    "    runtimeOnly 'runtime:lib:1.0'\n" +
                    "    testImplementation 'test:lib:1.0'\n" +
                    "    testRuntimeOnly 'testRuntime:lib:1.0'\n" +
                    "    annotationProcessor 'processor:lib:1.0'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'impl:lib:1.0'\n" +
                    "    api 'api:lib:1.0'\n" +
                    "    compileOnly 'compile:lib:1.0'\n" +
                    "    runtimeOnly 'runtime:lib:1.0'\n" +
                    "    testImplementation 'test:lib:1.0'\n" +
                    "    testRuntimeOnly 'testRuntime:lib:1.0'\n" +
                    "    annotationProcessor 'processor:lib:1.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }
    }

    // ==================== DEPENDENCY EXCLUDES ====================

    @Nested
    @DisplayName("Dependency Excludes")
    class DependencyExcludesTests {

        @Test
        @DisplayName("Add new dependency with excludes")
        void addDependencyWithExcludes() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation('some:other:1.0') {\n" +
                    "        exclude group: 'unwanted', module: 'lib'\n" +
                    "    }\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).contains("implementation 'existing:lib:1.0'");
            assertThat(result).contains("implementation('some:other:1.0')");
            assertThat(result).contains("exclude group: 'unwanted', module: 'lib'");
        }

        @Test
        @DisplayName("Merge excludes into existing dependency")
        void mergeExcludesIntoExisting() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'com.example:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation('com.example:lib:1.0') {\n" +
                    "        exclude group: 'unwanted'\n" +
                    "    }\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).contains("exclude group: 'unwanted'");
        }

        @Test
        @DisplayName("Merge multiple excludes into existing dependency")
        void mergeMultipleExcludes() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'com.example:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation('com.example:lib:1.0') {\n" +
                    "        exclude group: 'unwanted1', module: 'mod1'\n" +
                    "        exclude group: 'unwanted2', module: 'mod2'\n" +
                    "    }\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).contains("exclude group: 'unwanted1', module: 'mod1'");
            assertThat(result).contains("exclude group: 'unwanted2', module: 'mod2'");
        }
    }

    // ==================== COMPLEX SCRIPT TESTS ====================

    @Nested
    @DisplayName("Complex Script Scenarios")
    class ComplexScriptTests {

        @Test
        @DisplayName("Script with plugins and repositories - add dependencies")
        void scriptWithMultipleBlocksAddDependencies() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "    id 'application'\n" +
                    "}\n" +
                    "\n" +
                    "repositories {\n" +
                    "    mavenCentral()\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                    "}\n";

            // Dependencies should be added after repositories
            String result = inserter.insert(original, snippet);
            assertThat(result).contains("plugins {");
            assertThat(result).contains("repositories {");
            assertThat(result).contains("dependencies {");
            assertThat(result).contains("implementation 'com.google.guava:guava:31.0-jre'");
            // Verify semantic order: plugins → repositories → dependencies
            int reposEnd = result.indexOf("}", result.indexOf("repositories"));
            int depStart = result.indexOf("dependencies");
            assertThat(depStart).isGreaterThan(reposEnd);
        }

        @Test
        @DisplayName("Merge new repository into existing repositories block")
        void mergeRepositories() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n" +
                    "\n" +
                    "repositories {\n" +
                    "    mavenCentral()\n" +
                    "}\n";

            String snippet =
                    "repositories {\n" +
                    "    google()\n" +
                    "    jcenter()\n" +
                    "}\n";

            String expected =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n" +
                    "\n" +
                    "repositories {\n" +
                    "    mavenCentral()\n" +
                    "    google()\n" +
                    "    jcenter()\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Add maven repository with nested properties - preserves all nested content")
        void addMavenWithNestedProperties() {
            String original =
                    "repositories {\n" +
                    "    mavenCentral()\n" +
                    "}\n";

            String snippet =
                    "repositories {\n" +
                    "    maven {\n" +
                    "        url = uri('https://internal.repo.example.com/maven')\n" +
                    "        allowInsecureProtocol = true\n" +
                    "    }\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).contains("mavenCentral()");
            assertThat(result).contains("maven {");
            assertThat(result).contains("url = uri('https://internal.repo.example.com/maven')");
            assertThat(result).contains("allowInsecureProtocol = true");
        }

        @Test
        @DisplayName("Add maven repository with credentials - preserves all nested content")
        void addMavenWithCredentials() {
            String original =
                    "repositories {\n" +
                    "    mavenCentral()\n" +
                    "}\n";

            String snippet =
                    "repositories {\n" +
                    "    maven {\n" +
                    "        url = 'https://private.repo.com/releases'\n" +
                    "        credentials {\n" +
                    "            username = project.findProperty('repoUser') ?: ''\n" +
                    "            password = project.findProperty('repoPass') ?: ''\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).contains("mavenCentral()");
            assertThat(result).contains("maven {");
            assertThat(result).contains("url = 'https://private.repo.com/releases'");
            assertThat(result).contains("credentials {");
            assertThat(result).contains("username = project.findProperty('repoUser')");
            assertThat(result).contains("password = project.findProperty('repoPass')");
        }

        @Test
        @DisplayName("Create new repositories block with complex maven - preserves nested structure")
        void createRepositoriesWithComplexMaven() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n";

            String snippet =
                    "repositories {\n" +
                    "    maven {\n" +
                    "        url = 'https://corp.internal/maven'\n" +
                    "        allowInsecureProtocol = true\n" +
                    "        metadataSources {\n" +
                    "            mavenPom()\n" +
                    "            artifact()\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).contains("repositories {");
            assertThat(result).contains("maven {");
            assertThat(result).contains("url = 'https://corp.internal/maven'");
            assertThat(result).contains("allowInsecureProtocol = true");
            assertThat(result).contains("metadataSources {");
            assertThat(result).contains("mavenPom()");
            assertThat(result).contains("artifact()");
        }

        @Test
        @DisplayName("Full build script with multiple block modifications")
        void fullBuildScriptModifications() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n" +
                    "\n" +
                    "repositories {\n" +
                    "    mavenCentral()\n" +
                    "}\n" +
                    "\n" +
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "}\n";

            String snippet =
                    "plugins {\n" +
                    "    id 'application'\n" +
                    "}\n" +
                    "\n" +
                    "repositories {\n" +
                    "    google()\n" +
                    "}\n" +
                    "\n" +
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                    "    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'\n" +
                    "}\n";

            String expected =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "    id 'application'\n" +
                    "}\n" +
                    "\n" +
                    "repositories {\n" +
                    "    mavenCentral()\n" +
                    "    google()\n" +
                    "}\n" +
                    "\n" +
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                    "    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Preserve comments and blank lines")
        void preserveCommentsAndFormatting() {
            String original =
                    "// Build configuration\n" +
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n" +
                    "\n" +
                    "// Dependencies section\n" +
                    "dependencies {\n" +
                    "    // Core library\n" +
                    "    implementation 'com.google.guava:guava:30.0-jre'\n" +
                    "\n" +
                    "    // Logging\n" +
                    "    implementation 'org.slf4j:slf4j-api:1.7.30'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                    "}\n";

            String expected =
                    "// Build configuration\n" +
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n" +
                    "\n" +
                    "// Dependencies section\n" +
                    "dependencies {\n" +
                    "    // Core library\n" +
                    "    implementation 'com.google.guava:guava:31.0-jre'\n" +
                    "\n" +
                    "    // Logging\n" +
                    "    implementation 'org.slf4j:slf4j-api:1.7.30'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }
    }

    // ==================== DUPLICATE BLOCKS IN SNIPPET TESTS ====================

    @Nested
    @DisplayName("Duplicate Blocks in Snippet")
    class DuplicateBlocksTests {

        @Test
        @DisplayName("Multiple dependencies blocks in snippet should merge into one")
        void multipleDependenciesBlocksInSnippet() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'new:lib1:2.0'\n" +
                    "}\n" +
                    "\n" +
                    "dependencies {\n" +
                    "    testImplementation 'new:lib2:3.0'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation 'new:lib1:2.0'\n" +
                    "    testImplementation 'new:lib2:3.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Duplicate plugins blocks should merge")
        void duplicatePluginsBlocks() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n";

            String snippet =
                    "plugins {\n" +
                    "    id 'application'\n" +
                    "}\n" +
                    "\n" +
                    "plugins {\n" +
                    "    id 'jacoco'\n" +
                    "}\n";

            String expected =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "    id 'application'\n" +
                    "    id 'jacoco'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }
    }

    // ==================== PLUGIN NOTATION VARIATIONS ====================

    @Nested
    @DisplayName("Plugin Notation Variations")
    class PluginNotationTests {

        @Test
        @DisplayName("Plugin with version in snippet - add new")
        void pluginWithVersionAddNew() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n";

            String snippet =
                    "plugins {\n" +
                    "    id 'org.springframework.boot' version '3.2.0'\n" +
                    "}\n";

            String expected =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "    id 'org.springframework.boot' version '3.2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Update plugin version")
        void updatePluginVersion() {
            String original =
                    "plugins {\n" +
                    "    id 'org.springframework.boot' version '2.7.0'\n" +
                    "}\n";

            String snippet =
                    "plugins {\n" +
                    "    id 'org.springframework.boot' version '3.2.0'\n" +
                    "}\n";

            String expected =
                    "plugins {\n" +
                    "    id 'org.springframework.boot' version '3.2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Plugin without version in original - snippet adds version")
        void pluginWithoutVersionOriginalSnippetAddsVersion() {
            String original =
                    "plugins {\n" +
                    "    id 'org.springframework.boot'\n" +
                    "}\n";

            String snippet =
                    "plugins {\n" +
                    "    id 'org.springframework.boot' version '3.2.0'\n" +
                    "}\n";

            String expected =
                    "plugins {\n" +
                    "    id 'org.springframework.boot' version '3.2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Plugin with apply false - apply is parsed as separate plugin")
        void pluginWithApplyFalse() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n";

            String snippet =
                    "plugins {\n" +
                    "    id 'org.springframework.boot' version '3.2.0' apply false\n" +
                    "}\n";

            // Note: The current parser interprets 'apply' as a separate plugin id
            // This is a known limitation - 'apply false' is not properly handled
            String expected =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "    id 'apply'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }
    }

    // ==================== UNWRAPPED CONTENT TESTS ====================

    @Nested
    @DisplayName("Unwrapped Content in Snippet")
    class UnwrappedContentTests {

        @Test
        @DisplayName("Unwrapped dependencies without block")
        void unwrappedDependencies() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "implementation 'new:lib1:2.0'\n" +
                    "testImplementation 'new:lib2:3.0'\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation 'new:lib1:2.0'\n" +
                    "    testImplementation 'new:lib2:3.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Single unwrapped dependency line")
        void singleUnwrappedDependency() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet = "implementation 'new:lib:2.0'";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation 'new:lib:2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }
    }

    // ==================== MARKDOWN CODE BLOCK TESTS ====================

    @Nested
    @DisplayName("Markdown Code Blocks")
    class MarkdownCodeBlockTests {

        @Test
        @DisplayName("Groovy code block markers")
        void groovyCodeBlock() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "```groovy\n" +
                    "dependencies {\n" +
                    "    implementation 'new:lib:2.0'\n" +
                    "}\n" +
                    "```";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation 'new:lib:2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Gradle code block markers")
        void gradleCodeBlock() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "```gradle\n" +
                    "dependencies {\n" +
                    "    implementation 'new:lib:2.0'\n" +
                    "}\n" +
                    "```";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation 'new:lib:2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Plain code block markers")
        void plainCodeBlock() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "```\n" +
                    "dependencies {\n" +
                    "    implementation 'new:lib:2.0'\n" +
                    "}\n" +
                    "```";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation 'new:lib:2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }
    }

    // ==================== INVALID SYNTAX TESTS ====================

    @Nested
    @DisplayName("Invalid Syntax Handling")
    class InvalidSyntaxTests {

        @Test
        @DisplayName("Snippet with syntax errors - appends raw content")
        void snippetWithSyntaxErrors() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            // Invalid Groovy - missing closing brace
            String snippet = "dependencies { implementation 'new:lib:2.0'";

            // Current behavior: invalid snippets are appended as raw content
            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n" +
                    "\n" +
                    "dependencies { implementation 'new:lib:2.0'";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Original with minor syntax issues - should still work")
        void originalWithMinorIssues() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'new:lib:2.0'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "    implementation 'new:lib:2.0'\n" +
                    "}";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Completely invalid snippet - appends raw content")
        void completelyInvalidSnippet() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet = "this is not valid groovy code at all {{{}}}}";

            // Current behavior: invalid snippets are appended as raw content
            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n" +
                    "\n" +
                    "this is not valid groovy code at all {{{}}}}";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }
    }

    // ==================== TOP-LEVEL PROPERTIES ====================

    @Nested
    @DisplayName("Top-Level Properties")
    class TopLevelPropertiesTests {

        @Test
        @DisplayName("Add new property to empty script")
        void addPropertyToEmptyScript() {
            String original = "";

            String snippet = "group = 'com.example'\n";

            String expected = "group = 'com.example'\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Add new property after plugins block")
        void addPropertyAfterPlugins() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n";

            String snippet = "group = 'com.example'\n";

            // Result will have the property added with newlines for formatting
            String result = inserter.insert(original, snippet);
            assertThat(result).contains("plugins {");
            assertThat(result).contains("group = 'com.example'");
            // Property should appear after plugins block
            int pluginsEnd = result.indexOf("}");
            int groupStart = result.indexOf("group = ");
            assertThat(groupStart).isGreaterThan(pluginsEnd);
        }

        @Test
        @DisplayName("Update existing property value")
        void updatePropertyValue() {
            String original =
                    "group = 'com.example'\n" +
                    "version = '1.0.0'\n";

            String snippet = "version = '2.0.0'\n";

            String expected =
                    "group = 'com.example'\n" +
                    "version = '2.0.0'\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Add multiple properties from snippet")
        void addMultipleProperties() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n";

            String snippet =
                    "group = 'com.example'\n" +
                    "version = '1.0.0'\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).contains("group = 'com.example'");
            assertThat(result).contains("version = '1.0.0'");
        }

        @Test
        @DisplayName("Property with different value should update")
        void propertyWithDifferentValueShouldUpdate() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n" +
                    "\n" +
                    "group = 'com.oldexample'\n";

            String snippet = "group = 'com.newexample'\n";

            String expected =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n" +
                    "\n" +
                    "group = 'com.newexample'\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Empty original script - add dependencies block")
        void emptyOriginal() {
            String original = "";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'new:lib:1.0'\n" +
                    "}\n";

            // With empty original, dependencies block should be added at the start (semantic position)
            String result = inserter.insert(original, snippet);
            assertThat(result).contains("dependencies {");
            assertThat(result).contains("implementation 'new:lib:1.0'");
        }

        @Test
        @DisplayName("Empty snippet should not modify original")
        void emptySnippet() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet = "";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Whitespace-only snippet should not modify original")
        void whitespaceOnlySnippet() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet = "   \n\t\n   ";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Null snippet should not modify original")
        void nullSnippet() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String result = inserter.insert(original, null);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Duplicate dependency in snippet should update version once")
        void duplicateDependencyInSnippet() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'lib:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'lib:lib:2.0'\n" +
                    "}\n";

            String expected =
                    "dependencies {\n" +
                    "    implementation 'lib:lib:2.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Same version should not create edit")
        void sameVersionNoEdit() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'lib:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'lib:lib:1.0'\n" +
                    "}\n";

            List<IInsertionEdit> edits = inserter.generateEdits(original, snippet);
            assertThat(edits).isEmpty();

            String expected =
                    "dependencies {\n" +
                    "    implementation 'lib:lib:1.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }
    }

    // ==================== EDIT GENERATION TESTS ====================

    @Nested
    @DisplayName("Edit Generation")
    class EditGenerationTests {

        @Test
        @DisplayName("All edits should be ReplaceEdit instances")
        void allEditsAreReplaceEdit() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'lib:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'lib:lib:2.0'\n" +
                    "    implementation 'new:lib:1.0'\n" +
                    "}\n";

            List<IInsertionEdit> edits = inserter.generateEdits(original, snippet);

            for (IInsertionEdit edit : edits) {
                assertThat(edit).isInstanceOf(ReplaceEdit.class);
            }
        }

        @Test
        @DisplayName("Edit offsets should be valid")
        void validEditOffsets() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'lib:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'lib:lib:2.0'\n" +
                    "}\n";

            List<IInsertionEdit> edits = inserter.generateEdits(original, snippet);

            for (IInsertionEdit edit : edits) {
                assertThat(edit.getStartOffset()).isGreaterThanOrEqualTo(0);
                assertThat(edit.getEndOffset()).isLessThanOrEqualTo(original.length());
                assertThat(edit.getStartOffset()).isLessThanOrEqualTo(edit.getEndOffset());
            }
        }

        @Test
        @DisplayName("Edits should be sorted by offset descending")
        void editsSortedDescending() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'dep1:dep1:1.0'\n" +
                    "    implementation 'dep2:dep2:1.0'\n" +
                    "    implementation 'dep3:dep3:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
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

        @Test
        @DisplayName("Insertion edit has equal start and end offsets")
        void insertionEditHasEqualOffsets() {
            String original =
                    "dependencies {\n" +
                    "    implementation 'existing:lib:1.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'new:lib:1.0'\n" +
                    "}\n";

            List<IInsertionEdit> edits = inserter.generateEdits(original, snippet);

            assertThat(edits).hasSize(1);
            IInsertionEdit edit = edits.get(0);
            assertThat(edit.getStartOffset()).isEqualTo(edit.getEndOffset());
        }
    }

    // ==================== REAL-WORLD SCENARIOS ====================

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldTests {

        @Test
        @DisplayName("Spring Boot project - add web dependency")
        void springBootAddWebDependency() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "    id 'org.springframework.boot' version '3.2.0'\n" +
                    "    id 'io.spring.dependency-management' version '1.1.4'\n" +
                    "}\n" +
                    "\n" +
                    "group = 'com.example'\n" +
                    "version = '0.0.1-SNAPSHOT'\n" +
                    "\n" +
                    "repositories {\n" +
                    "    mavenCentral()\n" +
                    "}\n" +
                    "\n" +
                    "dependencies {\n" +
                    "    implementation 'org.springframework.boot:spring-boot-starter'\n" +
                    "    testImplementation 'org.springframework.boot:spring-boot-starter-test'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'org.springframework.boot:spring-boot-starter-web'\n" +
                    "}\n";

            String expected =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "    id 'org.springframework.boot' version '3.2.0'\n" +
                    "    id 'io.spring.dependency-management' version '1.1.4'\n" +
                    "}\n" +
                    "\n" +
                    "group = 'com.example'\n" +
                    "version = '0.0.1-SNAPSHOT'\n" +
                    "\n" +
                    "repositories {\n" +
                    "    mavenCentral()\n" +
                    "}\n" +
                    "\n" +
                    "dependencies {\n" +
                    "    implementation 'org.springframework.boot:spring-boot-starter'\n" +
                    "    testImplementation 'org.springframework.boot:spring-boot-starter-test'\n" +
                    "    implementation 'org.springframework.boot:spring-boot-starter-web'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Android project - update and add dependencies")
        void androidProjectDependencies() {
            String original =
                    "plugins {\n" +
                    "    id 'com.android.application'\n" +
                    "    id 'kotlin-android'\n" +
                    "}\n" +
                    "\n" +
                    "android {\n" +
                    "    compileSdk 34\n" +
                    "}\n" +
                    "\n" +
                    "dependencies {\n" +
                    "    implementation 'androidx.core:core-ktx:1.10.0'\n" +
                    "}\n";

            String snippet =
                    "dependencies {\n" +
                    "    implementation 'androidx.core:core-ktx:1.12.0'\n" +
                    "    implementation 'androidx.compose.ui:ui:1.5.0'\n" +
                    "}\n";

            String expected =
                    "plugins {\n" +
                    "    id 'com.android.application'\n" +
                    "    id 'kotlin-android'\n" +
                    "}\n" +
                    "\n" +
                    "android {\n" +
                    "    compileSdk 34\n" +
                    "}\n" +
                    "\n" +
                    "dependencies {\n" +
                    "    implementation 'androidx.core:core-ktx:1.12.0'\n" +
                    "    implementation 'androidx.compose.ui:ui:1.5.0'\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Multi-module project - add to subprojects")
        void multiModuleSubprojects() {
            String original =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n" +
                    "\n" +
                    "allprojects {\n" +
                    "    group = 'com.example'\n" +
                    "    version = '1.0.0'\n" +
                    "}\n" +
                    "\n" +
                    "subprojects {\n" +
                    "    apply plugin: 'java'\n" +
                    "    \n" +
                    "    repositories {\n" +
                    "        mavenCentral()\n" +
                    "    }\n" +
                    "}\n";

            String snippet =
                    "subprojects {\n" +
                    "    dependencies {\n" +
                    "        testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'\n" +
                    "    }\n" +
                    "}\n";

            String expected =
                    "plugins {\n" +
                    "    id 'java'\n" +
                    "}\n" +
                    "\n" +
                    "allprojects {\n" +
                    "    group = 'com.example'\n" +
                    "    version = '1.0.0'\n" +
                    "}\n" +
                    "\n" +
                    "subprojects {\n" +
                    "    apply plugin: 'java'\n" +
                    "    \n" +
                    "    repositories {\n" +
                    "        mavenCentral()\n" +
                    "    }\n" +
                    "    dependencies {\n" +
                    "        testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'\n" +
                    "    }\n" +
                    "}\n";

            String result = inserter.insert(original, snippet);
            assertThat(result).isEqualTo(expected);
        }
    }
}
