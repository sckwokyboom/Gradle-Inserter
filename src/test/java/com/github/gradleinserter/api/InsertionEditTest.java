package com.github.gradleinserter.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsertionEditTest {

    @Nested
    @DisplayName("InsertEdit")
    class InsertEditTests {

        @Test
        @DisplayName("Should create insert edit with valid offset")
        void validInsertEdit() {
            InsertEdit edit = new InsertEdit(10, "new text");

            assertThat(edit.getStartOffset()).isEqualTo(10);
            assertThat(edit.getEndOffset()).isEqualTo(10);
            assertThat(edit.getText()).isEqualTo("new text");
        }

        @Test
        @DisplayName("Should have startOffset equal to endOffset")
        void insertEditOffsetsEqual() {
            InsertEdit edit = new InsertEdit(50, "text");
            assertThat(edit.getStartOffset()).isEqualTo(edit.getEndOffset());
        }

        @Test
        @DisplayName("Should reject negative offset")
        void rejectNegativeOffset() {
            assertThatThrownBy(() -> new InsertEdit(-1, "text"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should allow custom description")
        void customDescription() {
            InsertEdit edit = new InsertEdit(10, "text", "Custom description");
            assertThat(edit.getDescription()).isEqualTo("Custom description");
        }

        @Test
        @DisplayName("Should have default description")
        void defaultDescription() {
            InsertEdit edit = new InsertEdit(10, "text");
            assertThat(edit.getDescription()).contains("10");
        }
    }

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
        @DisplayName("Should allow same start and end (insertion via replace)")
        void sameStartEnd() {
            ReplaceEdit edit = new ReplaceEdit(10, 10, "insert");
            assertThat(edit.getStartOffset()).isEqualTo(edit.getEndOffset());
        }
    }

    @Nested
    @DisplayName("Edit application")
    class EditApplicationTests {

        @Test
        @DisplayName("InsertEdit should be applicable to string")
        void applyInsertEdit() {
            String original = "Hello World";
            InsertEdit edit = new InsertEdit(5, " Beautiful");

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

        private String applyEdit(String original, IInsertionEdit edit) {
            StringBuilder sb = new StringBuilder(original);
            sb.replace(edit.getStartOffset(), edit.getEndOffset(), edit.getText());
            return sb.toString();
        }
    }
}
