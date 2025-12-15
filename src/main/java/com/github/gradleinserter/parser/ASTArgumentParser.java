package com.github.gradleinserter.parser;

import com.github.gradleinserter.view.ArgumentValue;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser for extracting structured argument values from Groovy expressions.
 * This replaces regex-based parsing with proper AST analysis.
 */
public class ASTArgumentParser {

    /**
     * Parse a Groovy expression and return its structured representation.
     *
     * @param source the source code to parse (e.g., "${version}", "1.0.0", "group: 'x', name: 'y'")
     * @return parsed ArgumentValue, or null if parsing fails
     */
    @Nullable
    public static ArgumentValue parse(@NotNull String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }

        try {
            Expression expr = parseExpression(source);
            if (expr != null) {
                return convertExpression(expr, source);
            }
        } catch (Exception e) {
            // If parsing fails, return as unknown
        }

        return ArgumentValue.unknown(source);
    }

    /**
     * Parse a source string into a Groovy Expression AST node.
     */
    @Nullable
    private static Expression parseExpression(@NotNull String source) {
        try {
            // Wrap in a dummy statement to make it parseable
            String wrapped = "def _ = " + source;

            CompilerConfiguration config = new CompilerConfiguration();
            config.setTolerance(10);

            CompilationUnit compilationUnit = new CompilationUnit(config);
            SourceUnit sourceUnit = compilationUnit.addSource("script.gradle", wrapped);

            compilationUnit.compile(Phases.CONVERSION);

            ModuleNode moduleNode = sourceUnit.getAST();
            BlockStatement block = moduleNode.getStatementBlock();

            if (block != null && !block.getStatements().isEmpty()) {
                Statement stmt = block.getStatements().get(0);
                if (stmt instanceof ExpressionStatement) {
                    Expression expr = ((ExpressionStatement) stmt).getExpression();
                    // It's a binary expression "def _ = <our_expr>"
                    if (expr instanceof BinaryExpression) {
                        return ((BinaryExpression) expr).getRightExpression();
                    }
                }
            }
        } catch (Exception e) {
            // Parsing failed, return null
        }

        return null;
    }

    /**
     * Convert a Groovy Expression to ArgumentValue.
     */
    @NotNull
    private static ArgumentValue convertExpression(@NotNull Expression expr, @NotNull String originalSource) {
        if (expr instanceof ConstantExpression) {
            Object value = ((ConstantExpression) expr).getValue();
            String strValue = value != null ? value.toString() : "";
            return ArgumentValue.constant(strValue);
        }

        if (expr instanceof GStringExpression) {
            GStringExpression gstring = (GStringExpression) expr;
            List<ArgumentValue.GStringPart> parts = parseGString(gstring);
            String reconstructed = reconstructGString(parts);
            return ArgumentValue.gstring(reconstructed, parts);
        }

        if (expr instanceof VariableExpression) {
            String name = ((VariableExpression) expr).getName();
            return ArgumentValue.variable(name);
        }

        if (expr instanceof PropertyExpression) {
            String text = extractPropertyText((PropertyExpression) expr);
            return ArgumentValue.property(text);
        }

        if (expr instanceof MethodCallExpression) {
            String text = expr.getText();
            return ArgumentValue.methodCall(text);
        }

        if (expr instanceof MapExpression) {
            return ArgumentValue.map(originalSource);
        }

        // Unknown or complex expression
        return ArgumentValue.unknown(originalSource);
    }

    /**
     * Parse GString into parts (literals and interpolations).
     */
    @NotNull
    private static List<ArgumentValue.GStringPart> parseGString(@NotNull GStringExpression gstring) {
        List<ConstantExpression> strings = gstring.getStrings();
        List<Expression> values = gstring.getValues();

        List<ArgumentValue.GStringPart> parts = new ArrayList<>();

        for (int i = 0; i < strings.size(); i++) {
            // Add literal part
            Object strValue = strings.get(i).getValue();
            if (strValue != null) {
                String str = strValue.toString();
                if (!str.isEmpty()) {
                    parts.add(ArgumentValue.GStringPart.literal(str));
                }
            }

            // Add interpolated expression
            if (i < values.size()) {
                Expression valExpr = values.get(i);
                String exprText = extractExpressionText(valExpr);
                parts.add(ArgumentValue.GStringPart.interpolation(exprText));
            }
        }

        return parts;
    }

    /**
     * Extract text representation of an expression, preserving nested braces.
     */
    @NotNull
    private static String extractExpressionText(@NotNull Expression expr) {
        if (expr instanceof VariableExpression) {
            return ((VariableExpression) expr).getName();
        }

        if (expr instanceof PropertyExpression) {
            return extractPropertyText((PropertyExpression) expr);
        }

        // For complex expressions, use getText()
        return expr.getText();
    }

    /**
     * Extract property access text (e.g., "obj.prop" or "obj.prop.nested").
     */
    @NotNull
    private static String extractPropertyText(@NotNull PropertyExpression expr) {
        Expression objectExpr = expr.getObjectExpression();
        Expression propExpr = expr.getProperty();

        String objectText;
        if (objectExpr instanceof PropertyExpression) {
            objectText = extractPropertyText((PropertyExpression) objectExpr);
        } else if (objectExpr instanceof VariableExpression) {
            objectText = ((VariableExpression) objectExpr).getName();
        } else {
            objectText = objectExpr.getText();
        }

        String propText;
        if (propExpr instanceof ConstantExpression) {
            propText = propExpr.getText();
        } else {
            propText = propExpr.getText();
        }

        return objectText + "." + propText;
    }

    /**
     * Reconstruct GString text with ${} syntax from parts.
     */
    @NotNull
    private static String reconstructGString(@NotNull List<ArgumentValue.GStringPart> parts) {
        StringBuilder sb = new StringBuilder();
        for (ArgumentValue.GStringPart part : parts) {
            if (part.isInterpolation()) {
                sb.append("${").append(part.getText()).append("}");
            } else {
                sb.append(part.getText());
            }
        }
        return sb.toString();
    }

    /**
     * Parse map-style notation and extract a specific key's value.
     * Example: "group: 'x', name: 'y', version: '1.0'" -> extractMapKey(..., "version") -> "1.0"
     *
     * @param source the map-style source string
     * @param key the key to extract
     * @return the value for the key, or empty string if not found
     */
    @NotNull
    public static String extractMapKey(@NotNull String source, @NotNull String key) {
        try {
            // Wrap as method call to parse named arguments
            String wrapped = "dummy(" + source + ")";
            Expression expr = parseExpression(wrapped);

            if (expr instanceof MethodCallExpression) {
                MethodCallExpression mce = (MethodCallExpression) expr;
                Expression args = mce.getArguments();

                // System.err.println("DEBUG: args class = " + (args != null ? args.getClass().getName() : "null"));

                if (args instanceof ArgumentListExpression) {
                    ArgumentListExpression argList = (ArgumentListExpression) args;
                    // System.err.println("DEBUG: argList size = " + argList.getExpressions().size());

                    for (Expression arg : argList.getExpressions()) {
                        // System.err.println("DEBUG: arg class = " + arg.getClass().getName());

                        // Handle MapExpression (named arguments become MapExpression)
                        if (arg instanceof MapExpression) {
                            MapExpression mapExpr = (MapExpression) arg;
                            for (MapEntryExpression entry : mapExpr.getMapEntryExpressions()) {
                                String entryKey = entry.getKeyExpression().getText();
                                if (key.equals(entryKey)) {
                                    Expression valueExpr = entry.getValueExpression();
                                    if (valueExpr instanceof ConstantExpression) {
                                        Object value = ((ConstantExpression) valueExpr).getValue();
                                        return value != null ? value.toString() : "";
                                    }
                                    return valueExpr.getText();
                                }
                            }
                        }
                        // Handle NamedArgumentListExpression
                        else if (arg instanceof NamedArgumentListExpression) {
                            NamedArgumentListExpression namedArgs = (NamedArgumentListExpression) arg;
                            for (MapEntryExpression entry : namedArgs.getMapEntryExpressions()) {
                                String entryKey = entry.getKeyExpression().getText();
                                if (key.equals(entryKey)) {
                                    Expression valueExpr = entry.getValueExpression();
                                    if (valueExpr instanceof ConstantExpression) {
                                        Object value = ((ConstantExpression) valueExpr).getValue();
                                        return value != null ? value.toString() : "";
                                    }
                                    return valueExpr.getText();
                                }
                            }
                        }
                    }
                } else if (args instanceof TupleExpression) {
                    // Sometimes args can be TupleExpression directly
                    TupleExpression tuple = (TupleExpression) args;
                    for (Expression arg : tuple.getExpressions()) {
                        if (arg instanceof MapExpression) {
                            MapExpression mapExpr = (MapExpression) arg;
                            for (MapEntryExpression entry : mapExpr.getMapEntryExpressions()) {
                                String entryKey = entry.getKeyExpression().getText();
                                if (key.equals(entryKey)) {
                                    Expression valueExpr = entry.getValueExpression();
                                    if (valueExpr instanceof ConstantExpression) {
                                        Object value = ((ConstantExpression) valueExpr).getValue();
                                        return value != null ? value.toString() : "";
                                    }
                                    return valueExpr.getText();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Parsing failed
            // System.err.println("DEBUG: Exception: " + e.getMessage());
        }

        return "";
    }

    /**
     * Parse exclude statement and extract group and module.
     * Example: "exclude group: 'com.example', module: 'lib'" -> {group: "com.example", module: "lib"}
     *
     * @param source the exclude statement source
     * @return array of [group, module], with nulls for missing values
     */
    @NotNull
    public static String[] parseExcludeStatement(@NotNull String source) {
        String[] result = new String[2]; // [group, module]

        try {
            // Try to parse as "exclude <map>"
            String afterExclude = source.trim();
            if (afterExclude.startsWith("exclude")) {
                afterExclude = afterExclude.substring("exclude".length()).trim();
            }

            result[0] = extractMapKey(afterExclude, "group");
            result[1] = extractMapKey(afterExclude, "module");

            // Convert empty strings to nulls
            if (result[0] != null && result[0].isEmpty()) {
                result[0] = null;
            }
            if (result[1] != null && result[1].isEmpty()) {
                result[1] = null;
            }
        } catch (Exception e) {
            // Parsing failed
        }

        return result;
    }

    /**
     * Extract the version string from plugin declaration source.
     * Example: "id 'plugin' version '1.0' apply false" -> "1.0"
     *
     * @param source the plugin declaration source
     * @return the version string, or null if not found
     */
    @Nullable
    public static String extractPluginVersion(@NotNull String source) {
        // Look for "version" keyword followed by a string literal
        int versionIdx = source.indexOf("version");
        if (versionIdx < 0) {
            return null;
        }

        // Extract the part after "version"
        String afterVersion = source.substring(versionIdx + "version".length()).trim();

        // Find the string literal or expression immediately after "version"
        // Look for opening quote or ${
        int quoteIdx = -1;
        int start = 0;

        // Skip whitespace
        while (start < afterVersion.length() && Character.isWhitespace(afterVersion.charAt(start))) {
            start++;
        }

        if (start >= afterVersion.length()) {
            return null;
        }

        char firstChar = afterVersion.charAt(start);

        // Handle quoted strings
        if (firstChar == '\'' || firstChar == '"') {
            char quote = firstChar;
            int end = start + 1;
            while (end < afterVersion.length() && afterVersion.charAt(end) != quote) {
                if (afterVersion.charAt(end) == '\\') {
                    end++; // Skip escaped character
                }
                end++;
            }
            if (end < afterVersion.length()) {
                return afterVersion.substring(start + 1, end);
            }
        }

        // Handle GString or variable reference
        if (firstChar == '$') {
            int end = start + 1;
            if (end < afterVersion.length() && afterVersion.charAt(end) == '{') {
                // ${...} format
                int braceDepth = 1;
                end++;
                while (end < afterVersion.length() && braceDepth > 0) {
                    if (afterVersion.charAt(end) == '{') {
                        braceDepth++;
                    } else if (afterVersion.charAt(end) == '}') {
                        braceDepth--;
                    }
                    end++;
                }
                return afterVersion.substring(start, end);
            } else {
                // $var format
                while (end < afterVersion.length() &&
                       (Character.isLetterOrDigit(afterVersion.charAt(end)) || afterVersion.charAt(end) == '_')) {
                    end++;
                }
                return afterVersion.substring(start, end);
            }
        }

        // Handle unquoted literal (rare but possible)
        int end = start;
        while (end < afterVersion.length() &&
               !Character.isWhitespace(afterVersion.charAt(end)) &&
               afterVersion.charAt(end) != ')' &&
               afterVersion.charAt(end) != ',') {
            end++;
        }
        if (end > start) {
            return afterVersion.substring(start, end);
        }

        return null;
    }
}
