package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

public class ClassProperty extends AstNode {

    {
        type = Token.EQ;
    }

    private AstNode key;
    private AstNode value; // Can be null!
    private boolean isStatic;

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth + 1));
        if (isStatic) {
            sb.append("static ");
        }
        if (isGetterMethod()) {
            sb.append("get ");
        } else if (isSetterMethod()) {
            sb.append("set ");
        }
        sb.append(key.toSource(depth));
        if (value != null) {
            if (type == Token.EQ) {
                sb.append(" = ");
            }
            sb.append(value.toSource(getType() == Token.EQ ? 0 : depth + 1));
            return sb.toString();
        }
        return sb.toString();
    }

    /** Visits this node, the left operand, and the right operand. */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            if (value != null) {
                value.visit(v);
            }
        }
    }

    /** Constructs a new {@code InfixExpression}. Updates bounds to include left and right nodes. */
    public ClassProperty(AstNode key, AstNode value) {
        setNameAndValue(key, value);
    }

    private void setNameAndValue(AstNode name, AstNode value) {
        assertNotNull(name);
        // compute our bounds while children have absolute positions
        int beg = name.getPosition();
        int end =
                value == null
                        ? name.getPosition() + name.getLength()
                        : value.getPosition() + value.getLength();
        setBounds(beg, end);

        // this updates their positions to be parent-relative
        setKey(name);
        setValue(value);
    }

    public AstNode getKey() {
        return key;
    }

    public void setKey(AstNode key) {
        assertNotNull(key);
        this.key = key;
        // line and column number should agree with source position
        setLineColumnNumber(key.getLineno(), key.getColumn());
        key.setParent(this);
    }

    public AstNode getValue() {
        return value;
    }

    public void setValue(AstNode value) {
        this.value = value;
        if (value != null) {
            value.setParent(this);
        }
    }

    /** Marks this node as a "getter" property. */
    public void setIsGetterMethod() {
        type = Token.GET;
    }

    /** Returns true if this is a getter function. */
    public boolean isGetterMethod() {
        return type == Token.GET;
    }

    /** Marks this node as a "setter" property. */
    public void setIsSetterMethod() {
        type = Token.SET;
    }

    /** Returns true if this is a setter function. */
    public boolean isSetterMethod() {
        return type == Token.SET;
    }

    public void setIsNormalMethod() {
        type = Token.METHOD;
    }

    public boolean isNormalMethod() {
        return type == Token.METHOD;
    }

    public boolean isMethod() {
        return isGetterMethod() || isSetterMethod() || isNormalMethod();
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }
}
