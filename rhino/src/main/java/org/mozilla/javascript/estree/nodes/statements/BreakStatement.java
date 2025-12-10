package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Identifier;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Break statement: exits from a loop or switch statement.
 *
 * <p>Example: {@code break;} or {@code break label;}
 */
public record BreakStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Identifier label // nullable - can be null for unlabeled break
        ) implements Statement {

    public BreakStatement {
        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "BreakStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
