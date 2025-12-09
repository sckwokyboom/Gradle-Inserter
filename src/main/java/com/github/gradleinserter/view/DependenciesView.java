package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.ir.IRNode;
import com.github.gradleinserter.ir.MethodCallNode;

import java.util.*;

/**
 * Semantic view of a dependencies block.
 */
public final class DependenciesView implements SemanticView {

    private final BlockNode blockNode;
    private final List<DependencyItem> dependencies;

    public DependenciesView(BlockNode blockNode) {
        this.blockNode = blockNode;
        this.dependencies = extractDependencies(blockNode);
    }

    /**
     * Create a synthetic view from a list of method calls (snippet without block wrapper).
     */
    public DependenciesView(List<MethodCallNode> calls) {
        this.blockNode = null;
        this.dependencies = new ArrayList<>();
        for (MethodCallNode call : calls) {
            if (isDependencyConfiguration(call.getMethodName())) {
                dependencies.add(DependencyItem.fromMethodCall(call));
            }
        }
    }

    private List<DependencyItem> extractDependencies(BlockNode block) {
        List<DependencyItem> result = new ArrayList<>();
        for (IRNode child : block.getChildren()) {
            if (child instanceof MethodCallNode) {
                MethodCallNode methodCall = (MethodCallNode) child;
                if (isDependencyConfiguration(methodCall.getMethodName())) {
                    result.add(DependencyItem.fromMethodCall(methodCall));
                }
            }
        }
        return result;
    }

    private static boolean isDependencyConfiguration(String name) {
        // Common Gradle dependency configurations
        return name.equals("implementation")
                || name.equals("api")
                || name.equals("compileOnly")
                || name.equals("runtimeOnly")
                || name.equals("testImplementation")
                || name.equals("testCompileOnly")
                || name.equals("testRuntimeOnly")
                || name.equals("annotationProcessor")
                || name.equals("kapt")
                || name.equals("ksp")
                // Legacy configurations
                || name.equals("compile")
                || name.equals("testCompile")
                || name.equals("runtime")
                || name.equals("provided")
                // Android configurations
                || name.equals("debugImplementation")
                || name.equals("releaseImplementation")
                || name.contains("Implementation")
                || name.contains("Api")
                || name.contains("CompileOnly")
                || name.contains("RuntimeOnly");
    }

    @Override
    public ViewType getType() {
        return ViewType.DEPENDENCIES;
    }

    @Override
    public IRNode getIRNode() {
        return blockNode;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public List<DependencyItem> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    /**
     * Find dependency by coordinates (group:name).
     */
    public Optional<DependencyItem> findByCoordinate(String group, String name) {
        return dependencies.stream()
                .filter(d -> Objects.equals(d.getGroup(), group) && Objects.equals(d.getName(), name))
                .findFirst();
    }

    /**
     * Find all dependencies with the same artifact (ignoring version).
     */
    public List<DependencyItem> findByArtifact(DependencyItem item) {
        List<DependencyItem> result = new ArrayList<>();
        for (DependencyItem dep : dependencies) {
            if (dep.sameArtifact(item)) {
                result.add(dep);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "DependenciesView{dependencies=" + dependencies.size() + "}";
    }
}
