package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Identifier;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Labeled statement: {@code label: body} Provides a label for break/continue statements. Example:
 * {@code loop: while (true) { break loop; }}
 */
public record LabeledStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Identifier label,
        Statement body)
        implements Statement {

    public LabeledStatement {
        if (label == null) throw new IllegalArgumentException("label required");
        if (body == null) throw new IllegalArgumentException("body required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "LabeledStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
