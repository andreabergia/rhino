package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Call expression: {@code callee(arguments)} Function call. Examples: {@code foo()}, {@code bar(1,
 * 2)}, {@code obj.method(x)}
 */
public record CallExpression(
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
        List<Expression> arguments, // "arguments" can include SpreadElement in ES6+
        boolean optional // true for callee?.() (ES2020 optional chaining)
        ) implements Expression {

    public CallExpression {
        if (callee == null) throw new IllegalArgumentException("callee required");
        if (arguments == null) throw new IllegalArgumentException("arguments required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        arguments = List.copyOf(arguments);
    }

    @Override
    public String type() {
        return "CallExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
