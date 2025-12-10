package org.mozilla.javascript;

import org.mozilla.javascript.ast.FunctionNode;

public class IRFunctionMetadata implements IRScriptOrFnMetadata {
    private final int index;
    private final int functionType;
    private final boolean hasFunctionName;
    private final String functionName;
    private final boolean inStrictMode;
    private final boolean isMethodDefinition;
    private final boolean isGenerator;
    private final boolean isES6Generator;
    private final boolean isShorthand;
    private final String sourceName;
    private final int rawSourceStart;
    private final int rawSourceEnd;

    private IRFunctionMetadata(
            int index,
            int functionType,
            boolean hasFunctionName,
            String functionName,
            boolean inStrictMode,
            boolean isMethodDefinition,
            boolean isGenerator,
            boolean isES6Generator,
            boolean isShorthand,
            String sourceName,
            int rawSourceStart,
            int rawSourceEnd) {
        this.index = index;
        this.functionType = functionType;
        this.hasFunctionName = hasFunctionName;
        this.functionName = functionName;
        this.inStrictMode = inStrictMode;
        this.isMethodDefinition = isMethodDefinition;
        this.isGenerator = isGenerator;
        this.isES6Generator = isES6Generator;
        this.isShorthand = isShorthand;
        this.sourceName = sourceName;
        this.rawSourceStart = rawSourceStart;
        this.rawSourceEnd = rawSourceEnd;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public int getFunctionType() {
        return functionType;
    }

    @Override
    public boolean isInStrictMode() {
        return inStrictMode;
    }

    @Override
    public boolean hasFunctionName() {
        return hasFunctionName;
    }

    @Override
    public String getFunctionName() {
        return functionName;
    }

    @Override
    public boolean isMethodDefinition() {
        return isMethodDefinition;
    }

    @Override
    public boolean isGenerator() {
        return isGenerator;
    }

    @Override
    public boolean isES6Generator() {
        return isES6Generator;
    }

    @Override
    public boolean isShorthand() {
        return isShorthand;
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public int getRawSourceStart() {
        return rawSourceStart;
    }

    @Override
    public int getRawSourceEnd() {
        return rawSourceEnd;
    }

    public static IRFunctionMetadata from(int index, FunctionNode functionNode) {
        return new IRFunctionMetadata(
                index,
                functionNode.getFunctionType(),
                functionNode.getName() != null,
                functionNode.getName(),
                functionNode.isInStrictMode(),
                functionNode.isMethodDefinition(),
                functionNode.isGenerator(),
                functionNode.isES6Generator(),
                functionNode.isShorthand(),
                functionNode.getSourceName(),
                functionNode.getRawSourceStart(),
                functionNode.getRawSourceEnd());
    }
}
