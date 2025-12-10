package org.mozilla.javascript;

import org.mozilla.javascript.ast.FunctionNode;

public class IRFunctionMetadata extends IRScriptOrFnMetadata {
    private final int index;
    private final int functionType;
    private final boolean isMethodDefinition;

    private IRFunctionMetadata(
            boolean inStrictMode, int index, int functionType, boolean isMethodDefinition) {
        super(inStrictMode);
        this.index = index;
        this.functionType = functionType;
        this.isMethodDefinition = isMethodDefinition;
    }

    public int getIndex() {
        return index;
    }

    public int getFunctionType() {
        return functionType;
    }

    public boolean isMethodDefinition() {
        return isMethodDefinition;
    }

    public static IRFunctionMetadata from(int index, FunctionNode functionNode) {
        return new IRFunctionMetadata(
                functionNode.isInStrictMode(),
                index,
                functionNode.getFunctionType(),
                functionNode.isMethodDefinition());
    }
}
