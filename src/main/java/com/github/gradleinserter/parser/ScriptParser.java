package com.github.gradleinserter.parser;

import com.github.gradleinserter.ir.IRNode;

import java.util.List;

/**
 * SPI for parsing build scripts into IR nodes.
 * Implementations exist for different DSLs (Groovy, Kotlin).
 */
public interface ScriptParser {

    /**
     * Parse the source into a list of IR nodes.
     *
     * @param source the script source code
     * @return list of top-level IR nodes
     */
    List<IRNode> parse(String source);

    /**
     * @return the DSL type this parser handles
     */
    DslType getDslType();

    enum DslType {
        GROOVY,
        KOTLIN
    }
}
