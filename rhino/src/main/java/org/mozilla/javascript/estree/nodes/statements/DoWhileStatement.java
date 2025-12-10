package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Do-while statement: loop that executes body at least once, then continues while test is truthy.
 *
 * <p>Example: {@code do { x--; } while (x > 0);}
 */
public record DoWhileStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Statement body,
        Expression test)
        implements Statement {

    public DoWhileStatement {
        if (body == null) throw new IllegalArgumentException("body required");
        if (test == null) throw new IllegalArgumentException("test required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "DoWhileStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
