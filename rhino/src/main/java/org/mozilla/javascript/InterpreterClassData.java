package org.mozilla.javascript;

final class InterpreterClassData {
    // TODO: what else? extends, properties, ...?
    private final int constructorFunctionId;

    InterpreterClassData(int constructorFunctionId) {
        this.constructorFunctionId = constructorFunctionId;
    }

    public int getConstructorFunctionId() {
        return constructorFunctionId;
    }
}
