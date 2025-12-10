package org.mozilla.javascript.estree.types;

/**
 * Logical operators for {@link org.mozilla.javascript.estree.nodes.expressions.LogicalExpression}.
 *
 * <p>Represents all valid logical operators in ESTree for short-circuit evaluation.
 */
public enum LogicalOperator {
    OR("||"),
    AND("&&"),
    NULLISH_COALESCING("??");

    private final String operator;

    LogicalOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return operator;
    }

    /** Parse a string operator into an enum constant. */
    public static LogicalOperator fromString(String operator) {
        return switch (operator) {
            case "||" -> OR;
            case "&&" -> AND;
            case "??" -> NULLISH_COALESCING;
            default -> throw new IllegalArgumentException("Unknown logical operator: " + operator);
        };
    }
}
