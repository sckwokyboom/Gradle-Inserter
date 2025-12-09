package com.github.gradleinserter.parser;

import com.github.gradleinserter.ir.*;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser for Groovy-based build.gradle scripts.
 * Converts Groovy AST to our intermediate representation.
 */
public class GroovyScriptParser implements ScriptParser {

    @Override
    public List<IRNode> parse(String source) {
        if (source == null || source.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            ModuleNode moduleNode = parseGroovy(source);
            return convertModule(moduleNode, source);
        } catch (Exception e) {
            // If parsing fails, return the whole source as a RawNode
            return Collections.singletonList(
                    new RawNode(0, source.length(), source)
            );
        }
    }

    @Override
    public DslType getDslType() {
        return DslType.GROOVY;
    }

    private ModuleNode parseGroovy(String source) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setTolerance(10); // Allow some errors

        CompilationUnit compilationUnit = new CompilationUnit(config);
        SourceUnit sourceUnit = compilationUnit.addSource("script.gradle", source);

        compilationUnit.compile(Phases.CONVERSION);

        return sourceUnit.getAST();
    }

    private List<IRNode> convertModule(ModuleNode module, String source) {
        List<IRNode> result = new ArrayList<>();

        BlockStatement block = module.getStatementBlock();
        if (block == null) {
            return result;
        }

        for (Statement stmt : block.getStatements()) {
            IRNode node = convertStatement(stmt, source);
            if (node != null) {
                result.add(node);
            }
        }

        return result;
    }

    private IRNode convertStatement(Statement stmt, String source) {
        if (stmt instanceof ExpressionStatement) {
            return convertExpression(((ExpressionStatement) stmt).getExpression(), source);
        }

        // For unknown statements, create a RawNode
        return createRawNode(stmt, source);
    }

    private IRNode convertExpression(Expression expr, String source) {
        if (expr instanceof MethodCallExpression) {
            return convertMethodCall((MethodCallExpression) expr, source);
        }

        if (expr instanceof BinaryExpression) {
            BinaryExpression binExpr = (BinaryExpression) expr;
            if (isAssignment(binExpr)) {
                return convertPropertyAssignment(binExpr, source);
            }
        }

        return createRawNode(expr, source);
    }

    private IRNode convertMethodCall(MethodCallExpression mce, String source) {
        // Handle chained method calls like: id 'plugin' version '1.0'
        // The outer method is 'version', the inner (object) is 'id'
        String methodName = extractMethodName(mce);
        Expression objectExpr = mce.getObjectExpression();

        // Check if this is a chained call
        if (objectExpr instanceof MethodCallExpression) {
            MethodCallExpression innerCall = (MethodCallExpression) objectExpr;
            String innerMethodName = extractMethodName(innerCall);

            // For plugin blocks, use the first method name (id, kotlin, etc.)
            if ("id".equals(innerMethodName) || "kotlin".equals(innerMethodName) || "java".equals(innerMethodName)) {
                // This is likely: id 'plugin' version '1.0' - return with full source spanning both calls
                List<String> args = extractArgumentsFromChainedCall(innerCall);
                return new MethodCallNode(
                        innerMethodName,
                        args,
                        null,
                        getStartOffset(innerCall, source),
                        getEndOffset(mce, source), // End at the outer call
                        extractSourceText(innerCall.getLineNumber(), innerCall.getColumnNumber(),
                                mce.getLastLineNumber(), mce.getLastColumnNumber(), source)
                );
            }
        }

        Expression arguments = mce.getArguments();

        // Check if this is a block call (method with closure argument)
        if (arguments instanceof ArgumentListExpression) {
            ArgumentListExpression argList = (ArgumentListExpression) arguments;
            List<Expression> exprs = argList.getExpressions();

            // Check for closure argument (block)
            if (!exprs.isEmpty() && exprs.get(exprs.size() - 1) instanceof ClosureExpression) {
                ClosureExpression closure = (ClosureExpression) exprs.get(exprs.size() - 1);
                return convertBlock(methodName, mce, closure, source);
            }

            // Regular method call with arguments
            List<String> args = extractArguments(argList);
            return new MethodCallNode(
                    methodName,
                    args,
                    null,
                    getStartOffset(mce, source),
                    getEndOffset(mce, source),
                    extractSourceText(mce, source)
            );
        }

        // Method call without arguments
        return new MethodCallNode(
                methodName,
                Collections.emptyList(),
                null,
                getStartOffset(mce, source),
                getEndOffset(mce, source),
                extractSourceText(mce, source)
        );
    }

    private List<String> extractArgumentsFromChainedCall(MethodCallExpression mce) {
        Expression arguments = mce.getArguments();
        if (arguments instanceof ArgumentListExpression) {
            return extractArguments((ArgumentListExpression) arguments);
        }
        return Collections.emptyList();
    }

    private String extractSourceText(int startLine, int startCol, int endLine, int endCol, String source) {
        int start = getOffset(source, startLine, startCol);
        int end = getOffset(source, endLine, endCol);
        if (start >= 0 && end <= source.length() && start < end) {
            return source.substring(start, end);
        }
        return "";
    }

    private BlockNode convertBlock(String name, MethodCallExpression mce,
                                   ClosureExpression closure, String source) {
        List<IRNode> children = new ArrayList<>();

        Statement code = closure.getCode();
        if (code instanceof BlockStatement) {
            BlockStatement block = (BlockStatement) code;
            for (Statement stmt : block.getStatements()) {
                IRNode child = convertStatement(stmt, source);
                if (child != null) {
                    children.add(child);
                }
            }
        }

        int startOffset = getStartOffset(mce, source);
        int endOffset = getEndOffset(mce, source);

        // Find body offsets (after '{' and before '}')
        int bodyStart = findBodyStart(closure, source, startOffset);
        int bodyEnd = findBodyEnd(closure, source, endOffset);

        return new BlockNode(
                name,
                startOffset,
                endOffset,
                bodyStart,
                bodyEnd,
                children,
                extractSourceText(mce, source)
        );
    }

    private IRNode convertPropertyAssignment(BinaryExpression binExpr, String source) {
        Expression left = binExpr.getLeftExpression();
        Expression right = binExpr.getRightExpression();

        String name = extractPropertyName(left);
        String value = extractSourceText(right, source);

        int startOffset = getStartOffset(binExpr, source);
        int endOffset = getEndOffset(binExpr, source);
        int valueStart = getStartOffset(right, source);
        int valueEnd = getEndOffset(right, source);

        return new PropertyNode(name, value, startOffset, endOffset, valueStart, valueEnd,
                extractSourceText(binExpr, source));
    }

    private String extractMethodName(MethodCallExpression mce) {
        Expression method = mce.getMethod();
        if (method instanceof ConstantExpression) {
            return ((ConstantExpression) method).getText();
        }
        return method.getText();
    }

    private List<String> extractArguments(ArgumentListExpression argList) {
        List<String> args = new ArrayList<>();
        for (Expression expr : argList.getExpressions()) {
            if (!(expr instanceof ClosureExpression)) {
                args.add(extractArgumentValue(expr));
            }
        }
        return args;
    }

    private String extractArgumentValue(Expression expr) {
        if (expr instanceof ConstantExpression) {
            Object value = ((ConstantExpression) expr).getValue();
            return value != null ? value.toString() : "";
        }
        if (expr instanceof GStringExpression) {
            return ((GStringExpression) expr).getText();
        }
        if (expr instanceof MethodCallExpression) {
            // For calls like project(':core')
            return expr.getText();
        }
        if (expr instanceof MapExpression) {
            return extractMapExpression((MapExpression) expr);
        }
        if (expr instanceof NamedArgumentListExpression) {
            return extractNamedArguments((NamedArgumentListExpression) expr);
        }
        return expr.getText();
    }

    private String extractMapExpression(MapExpression mapExpr) {
        StringBuilder sb = new StringBuilder();
        List<MapEntryExpression> entries = mapExpr.getMapEntryExpressions();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(", ");
            MapEntryExpression entry = entries.get(i);
            sb.append(entry.getKeyExpression().getText())
                    .append(": ")
                    .append(extractArgumentValue(entry.getValueExpression()));
        }
        return sb.toString();
    }

    private String extractNamedArguments(NamedArgumentListExpression namedArgs) {
        StringBuilder sb = new StringBuilder();
        List<MapEntryExpression> entries = namedArgs.getMapEntryExpressions();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(", ");
            MapEntryExpression entry = entries.get(i);
            sb.append(entry.getKeyExpression().getText())
                    .append(": ")
                    .append(extractArgumentValue(entry.getValueExpression()));
        }
        return sb.toString();
    }

    private String extractPropertyName(Expression expr) {
        if (expr instanceof VariableExpression) {
            return ((VariableExpression) expr).getName();
        }
        if (expr instanceof PropertyExpression) {
            PropertyExpression propExpr = (PropertyExpression) expr;
            return propExpr.getPropertyAsString();
        }
        return expr.getText();
    }

    private boolean isAssignment(BinaryExpression binExpr) {
        String op = binExpr.getOperation().getText();
        return "=".equals(op);
    }

    private int getStartOffset(ASTNode node, String source) {
        int line = node.getLineNumber();
        int col = node.getColumnNumber();

        if (line < 1 || col < 1) {
            return 0;
        }

        return getOffset(source, line, col);
    }

    private int getEndOffset(ASTNode node, String source) {
        int line = node.getLastLineNumber();
        int col = node.getLastColumnNumber();

        if (line < 1 || col < 1) {
            return source.length();
        }

        return getOffset(source, line, col);
    }

    private int getOffset(String source, int line, int col) {
        int offset = 0;
        int currentLine = 1;

        while (currentLine < line && offset < source.length()) {
            if (source.charAt(offset) == '\n') {
                currentLine++;
            }
            offset++;
        }

        return Math.min(offset + col - 1, source.length());
    }

    private int findBodyStart(ClosureExpression closure, String source, int blockStart) {
        // Find the '{' after block name
        int idx = source.indexOf('{', blockStart);
        if (idx >= 0) {
            return idx + 1; // Position after '{'
        }
        return blockStart;
    }

    private int findBodyEnd(ClosureExpression closure, String source, int blockEnd) {
        // Find the '}' - it's at blockEnd - 1 position
        int idx = source.lastIndexOf('}', blockEnd - 1);
        if (idx >= 0) {
            return idx; // Position of '}'
        }
        return blockEnd;
    }

    private String extractSourceText(ASTNode node, String source) {
        int start = getStartOffset(node, source);
        int end = getEndOffset(node, source);

        if (start >= 0 && end <= source.length() && start < end) {
            return source.substring(start, end);
        }
        return "";
    }

    private RawNode createRawNode(ASTNode node, String source) {
        int start = getStartOffset(node, source);
        int end = getEndOffset(node, source);
        String text = extractSourceText(node, source);
        return new RawNode(start, end, text);
    }
}
