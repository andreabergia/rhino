package org.mozilla.javascript.estree.nodes.clauses;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Node;
import org.mozilla.javascript.estree.nodes.base.Pattern;
import org.mozilla.javascript.estree.nodes.statements.BlockStatement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Catch clause: {@code catch (param) body} Represents the catch portion of a try-catch-finally
 * statement. Example: {@code catch (e) { console.error(e); }}
 */
public record CatchClause(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Pattern param, // nullable - ES2019+ allows catch without parameter
        BlockStatement body)
        implements Node {

    public CatchClause {
        if (body == null) throw new IllegalArgumentException("body required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "CatchClause";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
