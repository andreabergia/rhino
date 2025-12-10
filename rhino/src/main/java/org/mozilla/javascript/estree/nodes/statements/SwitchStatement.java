package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.nodes.clauses.SwitchCase;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Switch statement: multi-way branch based on value matching.
 *
 * <p>Example: {@code switch (x) { case 1: ...; break; default: ...; }}
 */
public record SwitchStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Expression discriminant,
        List<SwitchCase> cases)
        implements Statement {

    public SwitchStatement {
        if (discriminant == null) throw new IllegalArgumentException("discriminant required");
        if (cases == null) throw new IllegalArgumentException("cases required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        cases = List.copyOf(cases);
    }

    @Override
    public String type() {
        return "SwitchStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
