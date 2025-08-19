package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;

public class AstAndIrClassNode {
	public final ClassDefNode ast;
	public final Node ir;

	public AstAndIrClassNode(ClassDefNode ast, Node ir) {
		this.ast = ast;
		this.ir = ir;
	}
}
