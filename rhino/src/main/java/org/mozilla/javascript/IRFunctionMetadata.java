package org.mozilla.javascript;

import org.mozilla.javascript.ast.FunctionNode;

public class IRFunctionMetadata {
	private final FunctionNode fnNode;

	public IRFunctionMetadata(FunctionNode fnNode) {
		this.fnNode= fnNode;
	}


	public int getFunctionType() {
		return fnNode.getFunctionType();
	}
}
