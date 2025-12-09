package com.github.gradleinserter.ir;

import java.util.Objects;

/**
 * Represents a property assignment.
 * Examples:
 *   - version = '1.0.0'
 *   - group = 'com.example'
 *   - sourceCompatibility = JavaVersion.VERSION_11
 */
public final class PropertyNode extends AbstractIRNode {

    private final String name;
    private final String value;
    private final int valueStartOffset;
    private final int valueEndOffset;

    public PropertyNode(String name, String value,
                        int startOffset, int endOffset,
                        int valueStartOffset, int valueEndOffset,
                        String sourceText) {
        super(startOffset, endOffset, sourceText);
        this.name = Objects.requireNonNull(name, "name");
        this.value = Objects.requireNonNull(value, "value");
        this.valueStartOffset = valueStartOffset;
        this.valueEndOffset = valueEndOffset;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getValueStartOffset() {
        return valueStartOffset;
    }

    public int getValueEndOffset() {
        return valueEndOffset;
    }

    @Override
    public <T> T accept(IRNodeVisitor<T> visitor) {
        return visitor.visitProperty(this);
    }

    @Override
    public String toString() {
        return "PropertyNode{name='" + name + "', value='" + value + "'}";
    }
}
