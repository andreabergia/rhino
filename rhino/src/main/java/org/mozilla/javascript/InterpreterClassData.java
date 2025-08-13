package org.mozilla.javascript;

import java.util.List;

final class InterpreterClassData {
    // TODO: what else? extends, properties, ...?
    private final int constructorFunctionId;

    // TODO: migrate to an int[] for reduced memory usage
    private final List<Integer> memberFunctionIds;
    private final List<Integer> staticFunctionIds;

    InterpreterClassData(
            int constructorFunctionId,
            List<Integer> memberFunctionIds,
            List<Integer> staticFunctionIds) {
        this.constructorFunctionId = constructorFunctionId;
        this.memberFunctionIds = memberFunctionIds;
        this.staticFunctionIds = staticFunctionIds;
    }

    public int getConstructorFunctionId() {
        return constructorFunctionId;
    }

    public List<Integer> getMemberFunctionIds() {
        return memberFunctionIds;
    }

    public List<Integer> getStaticFunctionIds() {
        return staticFunctionIds;
    }
}
