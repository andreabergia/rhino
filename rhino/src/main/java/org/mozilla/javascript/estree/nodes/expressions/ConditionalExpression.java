package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Conditional expression: {@code test ? consequent : alternate} Ternary conditional operator.
 * Example: {@code x > 0 ? "positive" : "non-positive"}
 */
public record ConditionalExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Expression test,
        Expression consequent,
        Expression alternate)
        implements Expression {

    public ConditionalExpression {
        if (test == null) throw new IllegalArgumentException("test required");
        if (consequent == null) throw new IllegalArgumentException("consequent required");
        if (alternate == null) throw new IllegalArgumentException("alternate required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "ConditionalExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
