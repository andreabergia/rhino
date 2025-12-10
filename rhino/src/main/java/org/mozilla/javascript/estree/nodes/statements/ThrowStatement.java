package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Throw statement: throws an exception.
 *
 * <p>Example: {@code throw new Error("message");}
 */
public record ThrowStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Expression argument)
        implements Statement {

    public ThrowStatement {
        if (argument == null) throw new IllegalArgumentException("argument required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "ThrowStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
