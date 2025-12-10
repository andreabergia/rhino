package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Node;
import org.mozilla.javascript.estree.nodes.base.Pattern;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.nodes.declarations.VariableDeclaration;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * For-in statement: iterates over enumerable properties of an object.
 *
 * <p>Example: {@code for (var key in obj) { console.log(key); }}
 *
 * @see ForStatement
 */
public record ForInStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Node left, // VariableDeclaration or Pattern
        Expression right,
        Statement body)
        implements Statement {

    public ForInStatement {
        if (left == null) throw new IllegalArgumentException("left required");
        if (right == null) throw new IllegalArgumentException("right required");
        if (body == null) throw new IllegalArgumentException("body required");
        if (!(left instanceof VariableDeclaration) && !(left instanceof Pattern)) {
            throw new IllegalArgumentException("left must be VariableDeclaration or Pattern");
        }

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "ForInStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
