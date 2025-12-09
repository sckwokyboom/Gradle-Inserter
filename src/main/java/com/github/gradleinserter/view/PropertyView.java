package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.IRNode;
import com.github.gradleinserter.ir.PropertyNode;

/**
 * Semantic view for property assignments.
 * Example: version = '1.0.0', group = 'com.example'
 */
public final class PropertyView implements SemanticView {

    private final PropertyNode propertyNode;

    public PropertyView(PropertyNode propertyNode) {
        this.propertyNode = propertyNode;
    }

    @Override
    public ViewType getType() {
        return ViewType.PROPERTY;
    }

    @Override
    public IRNode getIRNode() {
        return propertyNode;
    }

    public PropertyNode getPropertyNode() {
        return propertyNode;
    }

    public String getName() {
        return propertyNode.getName();
    }

    public String getValue() {
        return propertyNode.getValue();
    }

    @Override
    public String toString() {
        return "PropertyView{" + getName() + " = " + getValue() + "}";
    }
}
