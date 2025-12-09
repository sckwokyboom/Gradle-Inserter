package com.github.gradleinserter.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a method call.
 * Examples:
 *   - implementation 'com.google:guava:31.0'
 *   - id 'java-library'
 *   - compile project(':core')
 */
public final class MethodCallNode extends AbstractIRNode {

    private final String methodName;
    private final List<String> arguments;
    private final IRNode closureBody; // may be null if no closure

    public MethodCallNode(String methodName, List<String> arguments,
                          IRNode closureBody,
                          int startOffset, int endOffset, String sourceText) {
        super(startOffset, endOffset, sourceText);
        this.methodName = Objects.requireNonNull(methodName, "methodName");
        this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
        this.closureBody = closureBody;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getArguments() {
        return arguments;
    }

    /**
     * @return the first argument as string, or empty string if no arguments
     */
    public String getFirstArgument() {
        return arguments.isEmpty() ? "" : arguments.get(0);
    }

    /**
     * @return the closure body if present, null otherwise
     */
    public IRNode getClosureBody() {
        return closureBody;
    }

    public boolean hasClosureBody() {
        return closureBody != null;
    }

    @Override
    public <T> T accept(IRNodeVisitor<T> visitor) {
        return visitor.visitMethodCall(this);
    }

    @Override
    public String toString() {
        return "MethodCallNode{methodName='" + methodName + "', arguments=" + arguments + "}";
    }
}
