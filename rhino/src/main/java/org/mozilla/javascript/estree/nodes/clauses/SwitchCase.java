package org.mozilla.javascript.estree.nodes.clauses;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Node;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Switch case clause: {@code case test: consequent} or {@code default: consequent} Represents a
 * case or default clause within a switch statement. Example: {@code case 1: return "one";} or
 * {@code default: return "other";}
 */
public record SwitchCase(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Expression test, // nullable - null for default case
        List<Statement> consequent // statements to execute
        ) implements Node {

    public SwitchCase {
        if (consequent == null) throw new IllegalArgumentException("consequent required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        consequent = List.copyOf(consequent);
    }

    @Override
    public String type() {
        return "SwitchCase";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
