package org.mozilla.javascript.estree.nodes.declarations;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Declaration;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Variable declaration: var/let/const declarations.
 *
 * <p>Declares one or more variables. Examples: {@code var x = 1;} {@code let y = 2, z = 3;} {@code
 * const PI = 3.14;}
 */
public record VariableDeclaration(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        List<VariableDeclarator> declarations,
        VariableDeclarationKind kind)
        implements Declaration {

    public VariableDeclaration {
        if (declarations == null) throw new IllegalArgumentException("declarations required");
        if (declarations.isEmpty())
            throw new IllegalArgumentException("declarations must not be empty");
        if (kind == null) throw new IllegalArgumentException("kind required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        declarations = List.copyOf(declarations);
    }

    @Override
    public String type() {
        return "VariableDeclaration";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
