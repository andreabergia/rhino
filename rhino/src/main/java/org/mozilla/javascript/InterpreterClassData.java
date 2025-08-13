package org.mozilla.javascript;

import java.util.List;

final class InterpreterClassData {
    // TODO: what else? extends, properties, ...?
    private final int constructorFunctionId;

	// TODO: migrate to an int[] for reduced memory usage
    private final List<Integer> memberFunctionIds;


    InterpreterClassData(int constructorFunctionId, List<Integer> memberFunctionIds) {
        this.constructorFunctionId = constructorFunctionId;
	    this.memberFunctionIds = memberFunctionIds;
    }

    public int getConstructorFunctionId() {
        return constructorFunctionId;
    }

	public List<Integer> getMemberFunctionIds() {
		return memberFunctionIds;
	}
}
