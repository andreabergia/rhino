package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.types.BinaryOperator;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Binary expression: {@code left operator right} Binary operation. Examples: {@code x + y}, {@code
 * a * b}, {@code foo == bar}, {@code x instanceof Array}
 */
public record BinaryExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        BinaryOperator operator,
        Expression left,
        Expression right)
        implements Expression {

    public BinaryExpression {
        if (operator == null) throw new IllegalArgumentException("operator required");
        if (left == null) throw new IllegalArgumentException("left required");
        if (right == null) throw new IllegalArgumentException("right required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "BinaryExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
