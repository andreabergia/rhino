package org.mozilla.javascript.ast;

import java.util.List;
import org.mozilla.javascript.Token;

public class ClassDefNode extends ScriptNode {
    private final Name className;
    private AstNode extendsNode; // Nullable
    private FunctionNode constructor; // Nullable
    private List<ClassProperty> properties;

    public ClassDefNode(int pos, Name name) {
        super(pos);
        this.className = name;
    }

    {
        type = Token.CLASS;
    }

    public Name getClassName() {
        return className;
    }

    public AstNode getExtendsNode() {
        return extendsNode;
    }

    public void setExtendsNode(AstNode extendsNode) {
        this.extendsNode = extendsNode;
    }

    public FunctionNode getConstructor() {
        return constructor;
    }

    public void setConstructor(FunctionNode constructor) {
        this.constructor = constructor;
    }

    public List<ClassProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<ClassProperty> properties) {
        this.properties = properties;
    }
}
