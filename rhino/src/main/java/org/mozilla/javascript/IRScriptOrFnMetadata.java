package org.mozilla.javascript;

// TODO Andrea should it be just an interface?
public abstract class IRScriptOrFnMetadata {
    private final boolean inStrictMode;

    protected IRScriptOrFnMetadata(boolean inStrictMode) {
        this.inStrictMode = inStrictMode;
    }

    public boolean isInStrictMode() {
        return inStrictMode;
    }

    public abstract int getFunctionType();

    public abstract boolean isMethodDefinition();

    public abstract boolean isGenerator();
}
