package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * While statement: loop that continues while test is truthy.
 *
 * <p>Example: {@code while (x > 0) { x--; }}
 */
public record WhileStatement(
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
        Statement body)
        implements Statement {

    public WhileStatement {
        if (test == null) throw new IllegalArgumentException("test required");
        if (body == null) throw new IllegalArgumentException("body required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "WhileStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
