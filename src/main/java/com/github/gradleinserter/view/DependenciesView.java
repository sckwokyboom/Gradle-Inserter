package com.github.gradleinserter.view;

import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.ir.IRNode;
import com.github.gradleinserter.ir.MethodCallNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Semantic view of a dependencies block.
 */
public final class DependenciesView implements SemanticView {

    @Nullable
    private final BlockNode blockNode;
    @NotNull
    private final List<DependencyItem> dependencies;

    public DependenciesView(@NotNull BlockNode blockNode) {
        this.blockNode = blockNode;
        this.dependencies = extractDependencies(blockNode);
    }

    /**
     * Create a synthetic view from a list of method calls (snippet without block wrapper).
     */
    public DependenciesView(@NotNull List<MethodCallNode> calls) {
        this.blockNode = null;
        this.dependencies = new ArrayList<>();
        for (MethodCallNode call : calls) {
            if (isDependencyConfiguration(call.getMethodName())) {
                dependencies.add(DependencyItem.fromMethodCall(call));
            }
        }
    }

    @NotNull
    private List<DependencyItem> extractDependencies(@NotNull BlockNode block) {
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

    private static boolean isDependencyConfiguration(@NotNull String name) {
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

    @NotNull
    @Override
    public ViewType getType() {
        return ViewType.DEPENDENCIES;
    }

    @Nullable
    @Override
    public IRNode getIRNode() {
        return blockNode;
    }

    @Nullable
    public BlockNode getBlockNode() {
        return blockNode;
    }

    @NotNull
    public List<DependencyItem> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    /**
     * Find dependency by coordinates (group:name).
     */
    @NotNull
    public Optional<DependencyItem> findByCoordinate(@NotNull String group, @NotNull String name) {
        return dependencies.stream()
                .filter(d -> Objects.equals(d.getGroup(), group) && Objects.equals(d.getName(), name))
                .findFirst();
    }

    /**
     * Find all dependencies with the same artifact (ignoring version).
     */
    @NotNull
    public List<DependencyItem> findByArtifact(@NotNull DependencyItem item) {
        List<DependencyItem> result = new ArrayList<>();
        for (DependencyItem dep : dependencies) {
            if (dep.sameArtifact(item)) {
                result.add(dep);
            }
        }
        return result;
    }

    /**
     * Find dependency with the same configuration and artifact (ignoring version).
     * This is more precise than findByArtifact - testImplementation and implementation
     * for the same artifact are treated as different dependencies.
     */
    @NotNull
    public Optional<DependencyItem> findByConfigurationAndArtifact(@NotNull DependencyItem item) {
        return dependencies.stream()
                .filter(dep -> dep.sameConfigurationAndArtifact(item))
                .findFirst();
    }

    @Override
    public String toString() {
        return "DependenciesView{dependencies=" + dependencies.size() + "}";
    }
}
