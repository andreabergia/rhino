package org.mozilla.javascript.estree.types;

/**
 * Assignment operators for {@link
 * org.mozilla.javascript.estree.nodes.expressions.AssignmentExpression}.
 *
 * <p>Represents all valid assignment operators in ESTree including simple assignment and compound
 * assignment operators.
 */
public enum AssignmentOperator {
    ASSIGN("="),
    ADD_ASSIGN("+="),
    SUB_ASSIGN("-="),
    MUL_ASSIGN("*="),
    DIV_ASSIGN("/="),
    MOD_ASSIGN("%="),
    LSH_ASSIGN("<<="),
    RSH_ASSIGN(">>="),
    URSH_ASSIGN(">>>="),
    BITOR_ASSIGN("|="),
    BITXOR_ASSIGN("^="),
    BITAND_ASSIGN("&="),
    EXP_ASSIGN("**="),
    OR_ASSIGN("||="),
    AND_ASSIGN("&&="),
    NULLISH_ASSIGN("??=");

    private final String operator;

    AssignmentOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return operator;
    }

    /** Parse a string operator into an enum constant. */
    public static AssignmentOperator fromString(String operator) {
        return switch (operator) {
            case "=" -> ASSIGN;
            case "+=" -> ADD_ASSIGN;
            case "-=" -> SUB_ASSIGN;
            case "*=" -> MUL_ASSIGN;
            case "/=" -> DIV_ASSIGN;
            case "%=" -> MOD_ASSIGN;
            case "<<=" -> LSH_ASSIGN;
            case ">>=" -> RSH_ASSIGN;
            case ">>>=" -> URSH_ASSIGN;
            case "|=" -> BITOR_ASSIGN;
            case "^=" -> BITXOR_ASSIGN;
            case "&=" -> BITAND_ASSIGN;
            case "**=" -> EXP_ASSIGN;
            case "||=" -> OR_ASSIGN;
            case "&&=" -> AND_ASSIGN;
            case "??=" -> NULLISH_ASSIGN;
            default ->
                    throw new IllegalArgumentException("Unknown assignment operator: " + operator);
        };
    }
}
