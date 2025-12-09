package org.mozilla.javascript;

import org.mozilla.javascript.ast.FunctionNode;

public class IRFunctionMetadata {
    private final int index;
    private final int functionType;

    public IRFunctionMetadata(int index, int functionType) {
        this.index = index;
        this.functionType = functionType;
    }

    public int getIndex() {
        return index;
    }

    public int getFunctionType() {
        return functionType;
    }

    public static IRFunctionMetadata from(int index, FunctionNode functionNode) {
        return new IRFunctionMetadata(index, functionNode.getFunctionType());
    }
}
