package com.github.gradleinserter;

import com.github.gradleinserter.ir.*;
import com.github.gradleinserter.parser.ScriptParser;
import com.github.gradleinserter.view.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes LLM-generated snippets that may contain:
 * - Complete blocks (dependencies { ... })
 * - Unwrapped content (just implementation 'x:y:z')
 * - Multiple blocks
 * - Invalid/partial Groovy code
 */
public class SnippetAnalyzer {

    private final ScriptParser parser;
    private final ViewExtractor viewExtractor;

    public SnippetAnalyzer(ScriptParser parser, ViewExtractor viewExtractor) {
        this.parser = parser;
        this.viewExtractor = viewExtractor;
    }

    /**
     * Analyze a snippet and extract semantic views.
     * Handles various snippet formats that LLMs might produce.
     */
    public List<SemanticView> analyze(String snippet) {
        if (snippet == null || snippet.trim().isEmpty()) {
            return new ArrayList<>();
        }

        snippet = preprocessSnippet(snippet);

        // Try to parse as-is first
        List<IRNode> nodes = parser.parse(snippet);
        List<SemanticView> views = viewExtractor.extract(nodes);

        // Check if we got meaningful results
        if (hasMeaningfulViews(views)) {
            return views;
        }

        // Try to detect and handle unwrapped content
        views = analyzeUnwrappedContent(nodes, snippet);
        if (!views.isEmpty()) {
            return views;
        }

        // Fallback: wrap the snippet and try again
        return analyzeFallback(snippet);
    }

    private String preprocessSnippet(String snippet) {
        // Remove markdown code block markers if present
        snippet = snippet.replaceAll("```groovy\\s*\\n?", "");
        snippet = snippet.replaceAll("```gradle\\s*\\n?", "");
        snippet = snippet.replaceAll("```\\s*\\n?", "");
        return snippet.trim();
    }

    private boolean hasMeaningfulViews(List<SemanticView> views) {
        for (SemanticView view : views) {
            if (view.getType() != SemanticView.ViewType.RAW) {
                return true;
            }
        }
        return false;
    }

    private List<SemanticView> analyzeUnwrappedContent(List<IRNode> nodes, String snippet) {
        List<SemanticView> result = new ArrayList<>();

        // Check if nodes look like dependency declarations
        List<MethodCallNode> dependencyLikeCalls = new ArrayList<>();
        List<MethodCallNode> pluginLikeCalls = new ArrayList<>();

        for (IRNode node : nodes) {
            if (node instanceof MethodCallNode) {
                MethodCallNode call = (MethodCallNode) node;
                if (isDependencyConfiguration(call.getMethodName())) {
                    dependencyLikeCalls.add(call);
                } else if (isPluginDeclaration(call.getMethodName())) {
                    pluginLikeCalls.add(call);
                }
            }
        }

        // Create synthetic views for unwrapped content
        if (!dependencyLikeCalls.isEmpty()) {
            result.add(new DependenciesView(dependencyLikeCalls));
        }

        if (!pluginLikeCalls.isEmpty()) {
            result.add(new PluginsView(pluginLikeCalls));
        }

        return result;
    }

    private List<SemanticView> analyzeFallback(String snippet) {
        List<SemanticView> result = new ArrayList<>();

        // Try wrapping in dependencies block
        String wrapped = "dependencies {\n" + snippet + "\n}";
        List<IRNode> nodes = parser.parse(wrapped);

        for (IRNode node : nodes) {
            if (node instanceof BlockNode) {
                BlockNode block = (BlockNode) node;
                if ("dependencies".equals(block.getName()) && !block.getChildren().isEmpty()) {
                    // Create a synthetic DependenciesView
                    List<MethodCallNode> calls = new ArrayList<>();
                    for (IRNode child : block.getChildren()) {
                        if (child instanceof MethodCallNode) {
                            calls.add((MethodCallNode) child);
                        }
                    }
                    if (!calls.isEmpty()) {
                        result.add(new DependenciesView(calls));
                        return result;
                    }
                }
            }
        }

        // If nothing worked, return raw view
        result.add(new RawView(new RawNode(0, snippet.length(), snippet)));
        return result;
    }

    private boolean isDependencyConfiguration(String name) {
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
                || name.equals("compile")
                || name.equals("testCompile")
                || name.equals("runtime")
                || name.equals("provided")
                || name.contains("Implementation")
                || name.contains("Api")
                || name.contains("CompileOnly");
    }

    private boolean isPluginDeclaration(String name) {
        return name.equals("id")
                || name.equals("kotlin")
                || name.equals("java")
                || name.equals("application");
    }
}
