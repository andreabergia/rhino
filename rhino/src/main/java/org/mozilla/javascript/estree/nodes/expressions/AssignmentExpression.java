package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Pattern;
import org.mozilla.javascript.estree.types.AssignmentOperator;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Assignment expression: {@code left operator right} Assignment operation. Examples: {@code x = 1},
 * {@code y += 2}, {@code z *= 3}
 */
public record AssignmentExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        AssignmentOperator operator,
        Pattern left, // Can be Pattern for destructuring assignment
        Expression right)
        implements Expression {

    public AssignmentExpression {
        if (operator == null) throw new IllegalArgumentException("operator required");
        if (left == null) throw new IllegalArgumentException("left required");
        if (right == null) throw new IllegalArgumentException("right required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "AssignmentExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
