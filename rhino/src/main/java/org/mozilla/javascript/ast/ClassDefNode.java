package org.mozilla.javascript.ast;

import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.Token;

public class ClassDefNode extends ScriptNode {
    private final Name className;
    private AstNode extendsNode; // Nullable
    private FunctionNode constructor; // Nullable
    private final List<ClassProperty> properties = new ArrayList<>();

    public ClassDefNode(int pos, Name name) {
        super(pos);
        this.className = name;
        if (className != null) {
            className.setParent(this);
        }
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
        assertNotNull(extendsNode);
        this.extendsNode = extendsNode;
        extendsNode.setParent(this);
    }

    public FunctionNode getConstructor() {
        return constructor;
    }

    public void setConstructor(FunctionNode constructor) {
        assertNotNull(constructor);
        this.constructor = constructor;
        constructor.setParent(this);
    }

    public List<ClassProperty> getProperties() {
        return properties;
    }

    public void addProperty(ClassProperty property) {
        // Note that duplicate names are allowed!
        assertNotNull(property);
        this.properties.add(property);
        property.setParent(this);
    }

    // TODO: toSource

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            if (className != null) {
                className.visit(v);
            }
            if (extendsNode != null) {
                extendsNode.visit(v);
            }
            if (constructor != null) {
                constructor.visit(v);
            }
            for (ClassProperty property : properties) {
                property.visit(v);
            }
        }
    }
}
