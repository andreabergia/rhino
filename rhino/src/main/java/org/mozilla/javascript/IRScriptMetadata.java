package org.mozilla.javascript;

import org.mozilla.javascript.ast.FunctionNode;

public class IRScriptMetadata implements IRScriptOrFnMetadata {
    private final boolean isInStrictMode;

    IRScriptMetadata(boolean inStrictMode) {
        isInStrictMode = inStrictMode;
    }

    @Override
    public int getFunctionType() {
        return FunctionNode.FUNCTION_SCRIPT;
    }

    @Override
    public boolean isInStrictMode() {
        return isInStrictMode;
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
