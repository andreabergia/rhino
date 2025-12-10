package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Expression statement: {@code expression;} Wraps an expression to be used as a statement. Example:
 * {@code x + y;} {@code foo();}
 */
public record ExpressionStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Expression expression)
        implements Statement {

    public ExpressionStatement {
        if (expression == null) throw new IllegalArgumentException("expression required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "ExpressionStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
