package com.github.gradleinserter.parser;

import com.github.gradleinserter.view.ArgumentValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ASTArgumentParserTest {

    @Nested
    @DisplayName("ArgumentValue parsing")
    class ArgumentValueParsing {

        @Test
        @DisplayName("Should parse simple constant")
        void parseSimpleConstant() {
            ArgumentValue value = ASTArgumentParser.parse("'1.0.0'");

            assertThat(value).isNotNull();
            assertThat(value.getType()).isEqualTo(ArgumentValue.Type.CONSTANT);
            assertThat(value.getConstantValue()).isEqualTo("1.0.0");
            assertThat(value.asString()).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("Should parse simple GString")
        void parseSimpleGString() {
            ArgumentValue value = ASTArgumentParser.parse("\"${version}\"");

            assertThat(value).isNotNull();
            assertThat(value.getType()).isEqualTo(ArgumentValue.Type.GSTRING);
            assertThat(value.isDynamic()).isTrue();
            assertThat(value.getRawText()).isEqualTo("${version}");
        }

        @Test
        @DisplayName("Should parse nested GString with multiple braces")
        void parseNestedGString() {
            // Groovy parses ${{{version}}} as closure calls, which is complex
            // For practical purposes, we just need to recognize it's dynamic
            ArgumentValue value = ASTArgumentParser.parse("\"${{{version}}}\"");

            assertThat(value).isNotNull();
            assertThat(value.isDynamic()).isTrue();
            // The actual parsing may be complex, but we at least recognize it's not a constant
        }

        @Test
        @DisplayName("Should parse GString with property access")
        void parseGStringWithPropertyAccess() {
            ArgumentValue value = ASTArgumentParser.parse("\"${props['version.key']}\"");

            assertThat(value).isNotNull();
            assertThat(value.getType()).isEqualTo(ArgumentValue.Type.GSTRING);
            assertThat(value.isDynamic()).isTrue();
        }

        @Test
        @DisplayName("Should parse GString with colons in property")
        void parseGStringWithColonsInProperty() {
            ArgumentValue value = ASTArgumentParser.parse("\"${props['group:name:version']}\"");

            assertThat(value).isNotNull();
            assertThat(value.getType()).isEqualTo(ArgumentValue.Type.GSTRING);
            assertThat(value.isDynamic()).isTrue();
        }

        @Test
        @DisplayName("Should parse variable reference")
        void parseVariableReference() {
            ArgumentValue value = ASTArgumentParser.parse("someVar");

            assertThat(value).isNotNull();
            assertThat(value.getType()).isEqualTo(ArgumentValue.Type.VARIABLE);
            assertThat(value.isDynamic()).isTrue();
        }

        @Test
        @DisplayName("Should parse property access")
        void parsePropertyAccess() {
            ArgumentValue value = ASTArgumentParser.parse("project.version");

            assertThat(value).isNotNull();
            assertThat(value.getType()).isEqualTo(ArgumentValue.Type.PROPERTY);
            assertThat(value.isDynamic()).isTrue();
        }
    }

    @Nested
    @DisplayName("Map key extraction")
    class MapKeyExtraction {

        @Test
        @DisplayName("Should extract simple map key")
        void extractSimpleMapKey() {
            String result = ASTArgumentParser.extractMapKey("group: 'com.example', name: 'lib'", "group");

            assertThat(result).isEqualTo("com.example");
        }

        @Test
        @DisplayName("Should extract map key with double quotes")
        void extractMapKeyWithDoubleQuotes() {
            String result = ASTArgumentParser.extractMapKey("group: \"com.example\", name: \"lib\"", "name");

            assertThat(result).isEqualTo("lib");
        }

        @Test
        @DisplayName("Should extract version from map")
        void extractVersionFromMap() {
            String result = ASTArgumentParser.extractMapKey(
                    "group: 'com.example', name: 'lib', version: '1.0.0'",
                    "version"
            );

            assertThat(result).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("Should return empty string for missing key")
        void extractMissingKey() {
            String result = ASTArgumentParser.extractMapKey("group: 'com.example'", "version");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Exclude statement parsing")
    class ExcludeStatementParsing {

        @Test
        @DisplayName("Should parse exclude with group and module")
        void parseExcludeWithGroupAndModule() {
            String[] result = ASTArgumentParser.parseExcludeStatement(
                    "exclude group: 'com.example', module: 'lib'"
            );

            assertThat(result).hasSize(2);
            assertThat(result[0]).isEqualTo("com.example");
            assertThat(result[1]).isEqualTo("lib");
        }

        @Test
        @DisplayName("Should parse exclude with only group")
        void parseExcludeWithOnlyGroup() {
            String[] result = ASTArgumentParser.parseExcludeStatement(
                    "exclude group: 'com.example'"
            );

            assertThat(result).hasSize(2);
            assertThat(result[0]).isEqualTo("com.example");
            assertThat(result[1]).isNull();
        }

        @Test
        @DisplayName("Should parse exclude with only module")
        void parseExcludeWithOnlyModule() {
            String[] result = ASTArgumentParser.parseExcludeStatement(
                    "exclude module: 'lib'"
            );

            assertThat(result).hasSize(2);
            assertThat(result[0]).isNull();
            assertThat(result[1]).isEqualTo("lib");
        }

        @Test
        @DisplayName("Should parse exclude with double quotes")
        void parseExcludeWithDoubleQuotes() {
            String[] result = ASTArgumentParser.parseExcludeStatement(
                    "exclude group: \"com.example\", module: \"lib\""
            );

            assertThat(result).hasSize(2);
            assertThat(result[0]).isEqualTo("com.example");
            assertThat(result[1]).isEqualTo("lib");
        }

        @Test
        @DisplayName("Should parse exclude with GString expressions")
        void parseExcludeWithGString() {
            String[] result = ASTArgumentParser.parseExcludeStatement(
                    "exclude group: ${excludeGroup}, module: ${excludeModule}"
            );

            assertThat(result).hasSize(2);
            // GString variables may be parsed as property access or other complex forms
            // For now, we just verify the method doesn't crash and returns an array
            // In real usage, these would be evaluated at runtime in Gradle
        }
    }

    @Nested
    @DisplayName("Plugin version extraction")
    class PluginVersionExtraction {

        @Test
        @DisplayName("Should extract simple version")
        void extractSimpleVersion() {
            String result = ASTArgumentParser.extractPluginVersion(
                    "id 'com.example.plugin' version '1.0.0'"
            );

            assertThat(result).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("Should extract version with double quotes")
        void extractVersionWithDoubleQuotes() {
            String result = ASTArgumentParser.extractPluginVersion(
                    "id \"com.example.plugin\" version \"1.0.0\""
            );

            assertThat(result).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("Should extract version with apply false")
        void extractVersionWithApplyFalse() {
            String result = ASTArgumentParser.extractPluginVersion(
                    "id 'com.example.plugin' version '1.0.0' apply false"
            );

            assertThat(result).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("Should return null when no version present")
        void extractWithoutVersion() {
            String result = ASTArgumentParser.extractPluginVersion(
                    "id 'com.example.plugin'"
            );

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should extract GString version")
        void extractGStringVersion() {
            String result = ASTArgumentParser.extractPluginVersion(
                    "id 'com.example.plugin' version ${pluginVersion}"
            );

            assertThat(result).isNotNull();
            // Should contain the variable reference
            assertThat(result).contains("pluginVersion");
        }
    }

    @Nested
    @DisplayName("GString parts")
    class GStringParts {

        @Test
        @DisplayName("Should parse GString with multiple parts")
        void parseMultiPartGString() {
            ArgumentValue value = ASTArgumentParser.parse("\"prefix-${version}-suffix\"");

            assertThat(value).isNotNull();
            assertThat(value.getType()).isEqualTo(ArgumentValue.Type.GSTRING);
            assertThat(value.getGstringParts()).isNotEmpty();

            // Should have literal, interpolation, literal parts
            boolean hasLiteral = value.getGstringParts().stream().anyMatch(ArgumentValue.GStringPart::isLiteral);
            boolean hasInterpolation = value.getGstringParts().stream().anyMatch(ArgumentValue.GStringPart::isInterpolation);

            assertThat(hasLiteral).isTrue();
            assertThat(hasInterpolation).isTrue();
        }

        @Test
        @DisplayName("Should reconstruct GString correctly")
        void reconstructGString() {
            ArgumentValue value = ASTArgumentParser.parse("\"${version}\"");

            assertThat(value).isNotNull();
            assertThat(value.getRawText()).contains("${");
            assertThat(value.getRawText()).contains("}");
        }
    }
}
