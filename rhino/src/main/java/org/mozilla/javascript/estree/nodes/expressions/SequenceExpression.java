package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Sequence expression: {@code expr1, expr2, expr3} Comma operator - evaluates expressions
 * left-to-right and returns the last one. Example: {@code (x++, y++, z)}
 */
public record SequenceExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        List<Expression> expressions)
        implements Expression {

    public SequenceExpression {
        if (expressions == null) throw new IllegalArgumentException("expressions required");
        if (expressions.isEmpty()) {
            throw new IllegalArgumentException("expressions must not be empty");
        }

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        expressions = List.copyOf(expressions);
    }

    @Override
    public String type() {
        return "SequenceExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
