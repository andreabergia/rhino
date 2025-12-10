package org.mozilla.javascript.estree.types;

/**
 * Binary operators for {@link org.mozilla.javascript.estree.nodes.expressions.BinaryExpression}.
 *
 * <p>Represents all valid binary operators in ESTree including arithmetic, comparison, bitwise, and
 * special operators.
 */
public enum BinaryOperator {
    EQ("=="),
    NE("!="),
    STRICT_EQ("==="),
    STRICT_NE("!=="),
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),
    LSH("<<"),
    RSH(">>"),
    URSH(">>>"),
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/"),
    MOD("%"),
    BITOR("|"),
    BITXOR("^"),
    BITAND("&"),
    IN("in"),
    INSTANCEOF("instanceof");

    private final String operator;

    BinaryOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return operator;
    }

    /** Parse a string operator into an enum constant. */
    public static BinaryOperator fromString(String operator) {
        return switch (operator) {
            case "==" -> EQ;
            case "!=" -> NE;
            case "===" -> STRICT_EQ;
            case "!==" -> STRICT_NE;
            case "<" -> LT;
            case "<=" -> LE;
            case ">" -> GT;
            case ">=" -> GE;
            case "<<" -> LSH;
            case ">>" -> RSH;
            case ">>>" -> URSH;
            case "+" -> ADD;
            case "-" -> SUB;
            case "*" -> MUL;
            case "/" -> DIV;
            case "%" -> MOD;
            case "|" -> BITOR;
            case "^" -> BITXOR;
            case "&" -> BITAND;
            case "in" -> IN;
            case "instanceof" -> INSTANCEOF;
            default -> throw new IllegalArgumentException("Unknown binary operator: " + operator);
        };
    }
}
