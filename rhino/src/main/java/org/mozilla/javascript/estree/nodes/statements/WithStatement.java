package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * With statement: {@code with (object) body} Extends the scope chain for a statement. Example:
 * {@code with (Math) { x = cos(0); }}
 */
public record WithStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Expression object,
        Statement body)
        implements Statement {

    public WithStatement {
        if (object == null) throw new IllegalArgumentException("object required");
        if (body == null) throw new IllegalArgumentException("body required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "WithStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
