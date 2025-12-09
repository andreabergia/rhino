package org.mozilla.javascript;

import org.mozilla.javascript.ast.FunctionNode;

public class IRFunctionMetadata {
	private final int functionType;

	public IRFunctionMetadata(int functionType) {
		this.functionType = functionType;
	}

	public int getFunctionType() {
		return functionType;
	}

	public static IRFunctionMetadata from(FunctionNode functionNode) {
		return new IRFunctionMetadata(
				functionNode.getFunctionType()
		);
	}
}
