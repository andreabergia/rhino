package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Update expression: {@code operator argument} or {@code argument operator} Increment or decrement
 * operation. Examples: {@code x++}, {@code ++x}, {@code x--}, {@code --x}
 */
public record UpdateExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        String operator, // "++" or "--"
        Expression argument,
        boolean prefix // true for ++x, false for x++
        ) implements Expression {

    public UpdateExpression {
        if (operator == null) throw new IllegalArgumentException("operator required");
        if (!operator.equals("++") && !operator.equals("--")) {
            throw new IllegalArgumentException("operator must be '++' or '--'");
        }
        if (argument == null) throw new IllegalArgumentException("argument required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "UpdateExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
