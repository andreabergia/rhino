package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Logical expression: {@code left operator right} Short-circuit logical operation. Examples: {@code
 * x && y}, {@code a || b}, {@code foo ?? bar} (ES2020)
 */
public record LogicalExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        String operator, // "||", "&&", "??" (nullish coalescing, ES2020)
        Expression left,
        Expression right)
        implements Expression {

    public LogicalExpression {
        if (operator == null) throw new IllegalArgumentException("operator required");
        if (!operator.equals("||") && !operator.equals("&&") && !operator.equals("??")) {
            throw new IllegalArgumentException("operator must be '||', '&&', or '??'");
        }
        if (left == null) throw new IllegalArgumentException("left required");
        if (right == null) throw new IllegalArgumentException("right required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "LogicalExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
