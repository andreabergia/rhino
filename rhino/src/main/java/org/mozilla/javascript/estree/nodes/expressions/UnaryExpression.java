package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Unary expression: {@code operator argument} Unary operation. Examples: {@code -x}, {@code +x},
 * {@code !x}, {@code ~x}, {@code typeof x}, {@code void x}, {@code delete x}
 */
public record UnaryExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        String operator, // "-", "+", "!", "~", "typeof", "void", "delete"
        boolean prefix, // always true for unary operators
        Expression argument)
        implements Expression {

    public UnaryExpression {
        if (operator == null) throw new IllegalArgumentException("operator required");
        if (argument == null) throw new IllegalArgumentException("argument required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "UnaryExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
