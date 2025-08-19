package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;

// TODO: collapse in AstAndIrClassNode
public final class IRClass {
    private final int classIndex;
    private final boolean isStatement;
    private final Node constructorIrNode;
    private final FunctionNode constructorAstNode;

    // TODO: hopefully we can get rid of at least one of the two constructor nodes
    public IRClass(
            int classIndex,
            boolean isStatement,
            Node constructorIrNode,
            FunctionNode constructorAstNode) {
        this.classIndex = classIndex;
        this.isStatement = isStatement;
        this.constructorIrNode = constructorIrNode;
        this.constructorAstNode = constructorAstNode;
    }

    public int getClassIndex() {
        return classIndex;
    }

    public boolean isStatement() {
        return isStatement;
    }

    public Node getConstructorIrNode() {
        return constructorIrNode;
    }

    public FunctionNode getConstructorAstNode() {
        return constructorAstNode;
    }
}
