package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Block statement: represents a block of code enclosed in braces.
 *
 * <p>Example: {@code { x = 1; y = 2; }}
 */
public record BlockStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        List<Statement> body)
        implements Statement {

    public BlockStatement {
        if (body == null) throw new IllegalArgumentException("body required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        body = List.copyOf(body);
    }

    @Override
    public String type() {
        return "BlockStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
