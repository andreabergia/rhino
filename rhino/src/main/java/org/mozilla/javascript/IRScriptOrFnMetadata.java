package org.mozilla.javascript;

public interface IRScriptOrFnMetadata {

    int getFunctionType();

    boolean isInStrictMode();

    boolean hasFunctionName();

    String getFunctionName();

    boolean isMethodDefinition();

    boolean isGenerator();

    boolean isES6Generator();

    boolean isShorthand();

    String getSourceName();

    int getRawSourceStart();

    int getRawSourceEnd();
}
