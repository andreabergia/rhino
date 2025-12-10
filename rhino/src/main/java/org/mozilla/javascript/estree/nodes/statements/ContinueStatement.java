package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Identifier;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Continue statement: continues to the next iteration of a loop.
 *
 * <p>Example: {@code continue;} or {@code continue label;}
 */
public record ContinueStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Identifier label // nullable - can be null for unlabeled continue
        ) implements Statement {

    public ContinueStatement {
        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "ContinueStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
