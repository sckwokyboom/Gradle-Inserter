package com.github.gradleinserter.view;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a parsed argument value from Groovy AST.
 * Can be a simple constant, a GString with interpolations, or a complex expression.
 * This replaces regex-based parsing with structured AST representation.
 */
public final class ArgumentValue {

    /**
     * Type of the argument value.
     */
    public enum Type {
        /** Simple string constant: 'value' or "value" */
        CONSTANT,
        /** GString with interpolations: "${version}" or "$var" */
        GSTRING,
        /** Map expression: [group: 'x', name: 'y'] or group: 'x', name: 'y' */
        MAP,
        /** Variable reference: someVar */
        VARIABLE,
        /** Property access: obj.property */
        PROPERTY,
        /** Method call: project(':core') */
        METHOD_CALL,
        /** Unknown/complex expression */
        UNKNOWN
    }

    @NotNull
    private final Type type;

    @NotNull
    private final String rawText;

    @Nullable
    private final String constantValue;

    @NotNull
    private final List<GStringPart> gstringParts;

    private ArgumentValue(@NotNull Type type, @NotNull String rawText,
                          @Nullable String constantValue, @NotNull List<GStringPart> gstringParts) {
        this.type = type;
        this.rawText = rawText;
        this.constantValue = constantValue;
        this.gstringParts = Collections.unmodifiableList(gstringParts);
    }

    /**
     * Create a constant value.
     */
    @NotNull
    public static ArgumentValue constant(@NotNull String value) {
        return new ArgumentValue(Type.CONSTANT, value, value, Collections.emptyList());
    }

    /**
     * Create a GString value with interpolations.
     */
    @NotNull
    public static ArgumentValue gstring(@NotNull String rawText, @NotNull List<GStringPart> parts) {
        return new ArgumentValue(Type.GSTRING, rawText, null, parts);
    }

    /**
     * Create a map expression value.
     */
    @NotNull
    public static ArgumentValue map(@NotNull String rawText) {
        return new ArgumentValue(Type.MAP, rawText, null, Collections.emptyList());
    }

    /**
     * Create a variable reference value.
     */
    @NotNull
    public static ArgumentValue variable(@NotNull String name) {
        return new ArgumentValue(Type.VARIABLE, name, null, Collections.emptyList());
    }

    /**
     * Create a property access value.
     */
    @NotNull
    public static ArgumentValue property(@NotNull String rawText) {
        return new ArgumentValue(Type.PROPERTY, rawText, null, Collections.emptyList());
    }

    /**
     * Create a method call value.
     */
    @NotNull
    public static ArgumentValue methodCall(@NotNull String rawText) {
        return new ArgumentValue(Type.METHOD_CALL, rawText, null, Collections.emptyList());
    }

    /**
     * Create an unknown/complex expression value.
     */
    @NotNull
    public static ArgumentValue unknown(@NotNull String rawText) {
        return new ArgumentValue(Type.UNKNOWN, rawText, null, Collections.emptyList());
    }

    @NotNull
    public Type getType() {
        return type;
    }

    /**
     * Get the raw text representation of this value.
     */
    @NotNull
    public String getRawText() {
        return rawText;
    }

    /**
     * Get the constant value if this is a CONSTANT type.
     */
    @Nullable
    public String getConstantValue() {
        return constantValue;
    }

    /**
     * Get the GString parts if this is a GSTRING type.
     */
    @NotNull
    public List<GStringPart> getGstringParts() {
        return gstringParts;
    }

    /**
     * Check if this value is a simple constant.
     */
    public boolean isConstant() {
        return type == Type.CONSTANT;
    }

    /**
     * Check if this value contains dynamic expressions (GString, variable, property, etc.).
     */
    public boolean isDynamic() {
        return type == Type.GSTRING || type == Type.VARIABLE ||
               type == Type.PROPERTY || type == Type.METHOD_CALL;
    }

    /**
     * Get a simple string representation of the value.
     * For constants, returns the constant value.
     * For dynamic values, returns the raw text.
     */
    @NotNull
    public String asString() {
        if (constantValue != null) {
            return constantValue;
        }
        return rawText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArgumentValue)) return false;
        ArgumentValue that = (ArgumentValue) o;
        return type == that.type && Objects.equals(rawText, that.rawText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, rawText);
    }

    @Override
    public String toString() {
        return "ArgumentValue{" +
                "type=" + type +
                ", rawText='" + rawText + '\'' +
                '}';
    }

    /**
     * Represents a part of a GString (either literal text or interpolated expression).
     */
    public static final class GStringPart {
        @NotNull
        private final String text;
        private final boolean isInterpolation;

        private GStringPart(@NotNull String text, boolean isInterpolation) {
            this.text = text;
            this.isInterpolation = isInterpolation;
        }

        /**
         * Create a literal text part.
         */
        @NotNull
        public static GStringPart literal(@NotNull String text) {
            return new GStringPart(text, false);
        }

        /**
         * Create an interpolated expression part (e.g., "version" from "${version}").
         */
        @NotNull
        public static GStringPart interpolation(@NotNull String expression) {
            return new GStringPart(expression, true);
        }

        @NotNull
        public String getText() {
            return text;
        }

        public boolean isInterpolation() {
            return isInterpolation;
        }

        public boolean isLiteral() {
            return !isInterpolation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GStringPart)) return false;
            GStringPart that = (GStringPart) o;
            return isInterpolation == that.isInterpolation && Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, isInterpolation);
        }

        @Override
        public String toString() {
            if (isInterpolation) {
                return "${" + text + "}";
            }
            return text;
        }
    }
}
