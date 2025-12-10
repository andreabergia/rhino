package org.mozilla.javascript;

import org.mozilla.javascript.ast.FunctionNode;

public class IRFunctionMetadata extends IRScriptOrFnMetadata {
    private final int index;
    private final int functionType;
    private final boolean isMethodDefinition;
    private final boolean isGenerator;

    private IRFunctionMetadata(
            boolean inStrictMode,
            int index,
            int functionType,
            boolean isMethodDefinition,
            boolean isGenerator) {
        super(inStrictMode);
        this.index = index;
        this.functionType = functionType;
        this.isMethodDefinition = isMethodDefinition;
        this.isGenerator = isGenerator;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public int getFunctionType() {
        return functionType;
    }

    @Override
    public boolean isMethodDefinition() {
        return isMethodDefinition;
    }

    @Override
    public boolean isGenerator() {
        return isGenerator;
    }

    public static IRFunctionMetadata from(int index, FunctionNode functionNode) {
        return new IRFunctionMetadata(
                functionNode.isInStrictMode(),
                index,
                functionNode.getFunctionType(),
                functionNode.isMethodDefinition(),
                functionNode.isGenerator());
    }
}
