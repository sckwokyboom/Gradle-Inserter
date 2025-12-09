package com.github.gradleinserter.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsertionEditTest {

    @Nested
    @DisplayName("ReplaceEdit")
    class ReplaceEditTests {

        @Test
        @DisplayName("Should create replace edit with valid range")
        void validReplaceEdit() {
            ReplaceEdit edit = new ReplaceEdit(10, 20, "replacement");

            assertThat(edit.getStartOffset()).isEqualTo(10);
            assertThat(edit.getEndOffset()).isEqualTo(20);
            assertThat(edit.getText()).isEqualTo("replacement");
        }

        @Test
        @DisplayName("Should reject invalid range")
        void rejectInvalidRange() {
            assertThatThrownBy(() -> new ReplaceEdit(20, 10, "text"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject negative start offset")
        void rejectNegativeStart() {
            assertThatThrownBy(() -> new ReplaceEdit(-1, 10, "text"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should allow zero-length replacement (deletion)")
        void zeroLengthReplacement() {
            ReplaceEdit edit = new ReplaceEdit(10, 20, "");

            assertThat(edit.getStartOffset()).isEqualTo(10);
            assertThat(edit.getEndOffset()).isEqualTo(20);
            assertThat(edit.getText()).isEmpty();
        }

        @Test
        @DisplayName("Should allow same start and end (pure insertion)")
        void sameStartEnd() {
            ReplaceEdit edit = new ReplaceEdit(10, 10, "insert");
            assertThat(edit.getStartOffset()).isEqualTo(edit.getEndOffset());
            assertThat(edit.getText()).isEqualTo("insert");
        }

        @Test
        @DisplayName("Should allow custom description")
        void customDescription() {
            ReplaceEdit edit = new ReplaceEdit(10, 20, "text", "Custom description");
            assertThat(edit.getDescription()).isEqualTo("Custom description");
        }

        @Test
        @DisplayName("Should have default description")
        void defaultDescription() {
            ReplaceEdit edit = new ReplaceEdit(10, 20, "text");
            assertThat(edit.getDescription()).contains("10");
            assertThat(edit.getDescription()).contains("20");
        }

        @Test
        @DisplayName("Should implement equals and hashCode correctly")
        void equalsAndHashCode() {
            ReplaceEdit edit1 = new ReplaceEdit(10, 20, "text");
            ReplaceEdit edit2 = new ReplaceEdit(10, 20, "text");
            ReplaceEdit edit3 = new ReplaceEdit(10, 20, "different");

            assertThat(edit1).isEqualTo(edit2);
            assertThat(edit1.hashCode()).isEqualTo(edit2.hashCode());
            assertThat(edit1).isNotEqualTo(edit3);
        }

        @Test
        @DisplayName("Should have meaningful toString")
        void toStringTest() {
            ReplaceEdit edit = new ReplaceEdit(10, 20, "text");
            String str = edit.toString();

            assertThat(str).contains("10");
            assertThat(str).contains("20");
            assertThat(str).contains("text");
        }
    }

    @Nested
    @DisplayName("Edit application")
    class EditApplicationTests {

        @Test
        @DisplayName("ReplaceEdit with same offsets should insert text")
        void applyInsertEdit() {
            String original = "Hello World";
            ReplaceEdit edit = new ReplaceEdit(5, 5, " Beautiful");

            String result = applyEdit(original, edit);

            assertThat(result).isEqualTo("Hello Beautiful World");
        }

        @Test
        @DisplayName("ReplaceEdit should replace text range")
        void applyReplaceEdit() {
            String original = "Hello World";
            ReplaceEdit edit = new ReplaceEdit(6, 11, "Universe");

            String result = applyEdit(original, edit);

            assertThat(result).isEqualTo("Hello Universe");
        }

        @Test
        @DisplayName("ReplaceEdit with empty text should delete")
        void applyDeleteEdit() {
            String original = "Hello Beautiful World";
            ReplaceEdit edit = new ReplaceEdit(5, 15, "");

            String result = applyEdit(original, edit);

            assertThat(result).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("Insert at beginning")
        void insertAtBeginning() {
            String original = "World";
            ReplaceEdit edit = new ReplaceEdit(0, 0, "Hello ");

            String result = applyEdit(original, edit);

            assertThat(result).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("Insert at end")
        void insertAtEnd() {
            String original = "Hello";
            ReplaceEdit edit = new ReplaceEdit(5, 5, " World");

            String result = applyEdit(original, edit);

            assertThat(result).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("Multiple edits applied in reverse order")
        void multipleEditsReverseOrder() {
            String original = "AAABBBCCC";
            // Replace BBB with XXX
            ReplaceEdit edit1 = new ReplaceEdit(3, 6, "XXX");
            // Replace CCC with YYY
            ReplaceEdit edit2 = new ReplaceEdit(6, 9, "YYY");

            // Apply in reverse order (from end to start)
            String result = applyEdit(original, edit2);
            result = applyEdit(result, edit1);

            assertThat(result).isEqualTo("AAAXXXYYY");
        }

        private String applyEdit(String original, IInsertionEdit edit) {
            StringBuilder sb = new StringBuilder(original);
            sb.replace(edit.getStartOffset(), edit.getEndOffset(), edit.getText());
            return sb.toString();
        }
    }
}
