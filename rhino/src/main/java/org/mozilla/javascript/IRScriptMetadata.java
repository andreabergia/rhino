package org.mozilla.javascript;

import org.mozilla.javascript.ast.FunctionNode;

public class IRScriptMetadata extends IRScriptOrFnMetadata {
    protected IRScriptMetadata(boolean inStrictMode) {
        super(inStrictMode);
    }

    @Override
    public int getFunctionType() {
        return FunctionNode.FUNCTION_SCRIPT;
    }

    @Override
    public boolean isMethodDefinition() {
        return false;
    }

    @Override
    public boolean isGenerator() {
        return false;
    }
}
