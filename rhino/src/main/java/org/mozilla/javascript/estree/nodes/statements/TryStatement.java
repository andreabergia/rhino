package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.nodes.clauses.CatchClause;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Try statement: exception handling construct.
 *
 * <p>Example: {@code try { foo(); } catch (e) { bar(); } finally { cleanup(); }}
 */
public record TryStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        BlockStatement block,
        CatchClause handler, // nullable - can be null if no catch clause
        BlockStatement finalizer // nullable - can be null if no finally clause
        ) implements Statement {

    public TryStatement {
        if (block == null) throw new IllegalArgumentException("block required");
        if (handler == null && finalizer == null) {
            throw new IllegalArgumentException("either handler or finalizer required");
        }

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "TryStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
