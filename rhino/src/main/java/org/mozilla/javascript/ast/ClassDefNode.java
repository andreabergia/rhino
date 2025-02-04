package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

public class ClassDefNode extends ScriptNode {
	private final Name className;

	public ClassDefNode(
			int pos,
			Name name
	) {
		super(pos);
		this.className = name;
	}

	{
		type = Token.CLASS;
	}

	public Name getClassName() {
		return className;
	}
}
