package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * If statement: conditional execution of statements.
 *
 * <p>Example: {@code if (x > 0) foo(); else bar();}
 */
public record IfStatement(
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
        Statement consequent,
        Statement alternate // nullable - can be null if no else clause
        ) implements Statement {

    public IfStatement {
        if (test == null) throw new IllegalArgumentException("test required");
        if (consequent == null) throw new IllegalArgumentException("consequent required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "IfStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
