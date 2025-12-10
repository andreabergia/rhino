package org.mozilla.javascript;

import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.ScriptNode;

public class IRScriptMetadata implements IRScriptOrFnMetadata {
    private final boolean inStrictMode;
    private final String sourceName;
    private final int rawSourceLength;

    private IRScriptMetadata(boolean inStrictMode, String sourceName, int rawSourceLength) {
        this.inStrictMode = inStrictMode;
        this.sourceName = sourceName;
        this.rawSourceLength = rawSourceLength;
    }

    @Override
    public int getFunctionType() {
        return FunctionNode.FUNCTION_SCRIPT;
    }

    @Override
    public boolean isInStrictMode() {
        return inStrictMode;
    }

    @Override
    public boolean hasFunctionName() {
        return false;
    }

    @Override
    public String getFunctionName() {
        return "";
    }

    @Override
    public boolean isMethodDefinition() {
        return false;
    }

    @Override
    public boolean isGenerator() {
        return false;
    }

    @Override
    public boolean isES6Generator() {
        return false;
    }

    @Override
    public boolean isShorthand() {
        return false;
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public int getRawSourceStart() {
        return 0;
    }

    @Override
    public int getRawSourceEnd() {
        return rawSourceLength;
    }

    public static IRScriptMetadata from(ScriptNode scriptNode, int rawSourceLength) {
        return new IRScriptMetadata(
                scriptNode.isInStrictMode(), scriptNode.getSourceName(), rawSourceLength);
    }
}
