package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Array expression: array literals with optional null elements for holes.
 *
 * <p>Example: {@code [1, 2, 3]} or {@code [1, , 3]} (with hole at index 1)
 */
public record ArrayExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        List<Expression> elements // List may contain null for array holes
        ) implements Expression {

    public ArrayExpression {
        if (elements == null) throw new IllegalArgumentException("elements required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        elements = List.copyOf(elements);
    }

    @Override
    public String type() {
        return "ArrayExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
