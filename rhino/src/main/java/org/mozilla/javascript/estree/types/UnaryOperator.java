package org.mozilla.javascript.estree.types;

/**
 * Unary operators for {@link org.mozilla.javascript.estree.nodes.expressions.UnaryExpression}.
 *
 * <p>Represents all valid unary operators in ESTree: {@code -x}, {@code +x}, {@code !x}, {@code
 * ~x}, {@code typeof x}, {@code void x}, {@code delete x}
 */
public enum UnaryOperator {
    MINUS("-"),
    PLUS("+"),
    NOT("!"),
    BITWISE_NOT("~"),
    TYPEOF("typeof"),
    VOID("void"),
    DELETE("delete");

    private final String operator;

    UnaryOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return operator;
    }

    /** Parse a string operator into an enum constant. */
    public static UnaryOperator fromString(String operator) {
        return switch (operator) {
            case "-" -> MINUS;
            case "+" -> PLUS;
            case "!" -> NOT;
            case "~" -> BITWISE_NOT;
            case "typeof" -> TYPEOF;
            case "void" -> VOID;
            case "delete" -> DELETE;
            default -> throw new IllegalArgumentException("Unknown unary operator: " + operator);
        };
    }
}
