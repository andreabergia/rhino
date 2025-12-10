package org.mozilla.javascript;

public interface IRScriptOrFnMetadata {

    boolean isInStrictMode();

    int getFunctionType();

    boolean isMethodDefinition();

    boolean isGenerator();
}
