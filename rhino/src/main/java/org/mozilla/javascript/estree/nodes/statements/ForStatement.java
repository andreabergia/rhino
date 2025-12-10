package org.mozilla.javascript.estree.nodes.statements;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Node;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.nodes.declarations.VariableDeclaration;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * For statement: traditional C-style for loop.
 *
 * <p>Example: {@code for (var i = 0; i < 10; i++) { console.log(i); }}
 *
 * @see ForInStatement
 */
public record ForStatement(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Node init, // nullable - can be VariableDeclaration or Expression or null
        Expression test, // nullable - can be null for infinite loop
        Expression update, // nullable - can be null
        Statement body)
        implements Statement {

    public ForStatement {
        if (body == null) throw new IllegalArgumentException("body required");
        if (init != null
                && !(init instanceof VariableDeclaration)
                && !(init instanceof Expression)) {
            throw new IllegalArgumentException(
                    "init must be VariableDeclaration, Expression, or null");
        }

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "ForStatement";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
