package org.mozilla.javascript;

public abstract class IRScriptOrFnMetadata {
    private final boolean inStrictMode;

    protected IRScriptOrFnMetadata(boolean inStrictMode) {
        this.inStrictMode = inStrictMode;
    }

    public boolean isInStrictMode() {
        return inStrictMode;
    }
}
