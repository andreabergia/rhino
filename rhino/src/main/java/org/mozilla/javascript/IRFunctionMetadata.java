package org.mozilla.javascript;

import org.mozilla.javascript.ast.FunctionNode;

public class IRFunctionMetadata implements IRScriptOrFnMetadata {
    private final int index;
    private final int functionType;
    private final boolean isInStrictMode;
    private final boolean isMethodDefinition;
    private final boolean isGenerator;

    private IRFunctionMetadata(
            int index,
            int functionType,
            boolean isInStrictMode,
            boolean isMethodDefinition,
            boolean isGenerator) {
        this.index = index;
        this.functionType = functionType;
        this.isInStrictMode = isInStrictMode;
        this.isMethodDefinition = isMethodDefinition;
        this.isGenerator = isGenerator;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean isInStrictMode() {
        return isInStrictMode;
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
                index,
                functionNode.getFunctionType(),
                functionNode.isInStrictMode(),
                functionNode.isMethodDefinition(),
                functionNode.isGenerator());
    }
}
