package org.mozilla.javascript.estree.types;

/**
 * Update operators for {@link org.mozilla.javascript.estree.nodes.expressions.UpdateExpression}.
 *
 * <p>Represents the two valid update operators in ESTree: {@code ++} and {@code --}
 */
public enum UpdateOperator {
    INCREMENT("++"),
    DECREMENT("--");

    private final String operator;

    UpdateOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return operator;
    }

    /** Parse a string operator into an enum constant. */
    public static UpdateOperator fromString(String operator) {
        return switch (operator) {
            case "++" -> INCREMENT;
            case "--" -> DECREMENT;
            default -> throw new IllegalArgumentException("Unknown update operator: " + operator);
        };
    }
}
