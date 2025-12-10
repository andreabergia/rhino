package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Identifier;
import org.mozilla.javascript.estree.nodes.base.Pattern;
import org.mozilla.javascript.estree.nodes.statements.BlockStatement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Function expression: {@code function [id](params) body} Anonymous or named function expression.
 * Example: {@code function(x) { return x * 2; }} or {@code function factorial(n) { return n *
 * factorial(n-1); }}
 */
public record FunctionExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Identifier id, // nullable - can be null for anonymous function
        List<Pattern> params,
        BlockStatement body,
        boolean generator, // true for function*
        boolean async // true for async function (ES2017+)
        ) implements Expression {

    public FunctionExpression {
        if (params == null) throw new IllegalArgumentException("params required");
        if (body == null) throw new IllegalArgumentException("body required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        params = List.copyOf(params);
    }

    @Override
    public String type() {
        return "FunctionExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
