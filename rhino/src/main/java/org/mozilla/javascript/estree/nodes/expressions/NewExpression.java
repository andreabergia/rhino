package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * New expression: {@code new callee(arguments)} Object instantiation. Examples: {@code new Date()},
 * {@code new Array(10)}, {@code new Person("John")}
 */
public record NewExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Expression callee,
        List<Expression> arguments // "arguments" can include SpreadElement in ES6+
        ) implements Expression {

    public NewExpression {
        if (callee == null) throw new IllegalArgumentException("callee required");
        if (arguments == null) throw new IllegalArgumentException("arguments required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        arguments = List.copyOf(arguments);
    }

    @Override
    public String type() {
        return "NewExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
