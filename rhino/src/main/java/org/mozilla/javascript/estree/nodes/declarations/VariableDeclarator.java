package org.mozilla.javascript.estree.nodes.declarations;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Node;
import org.mozilla.javascript.estree.nodes.base.Pattern;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Variable declarator: {@code id [= init]} Individual variable in a variable declaration. Examples:
 * {@code x = 1}, {@code y}, {@code {a, b} = obj}
 */
public record VariableDeclarator(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Pattern id, // Can be Identifier or destructuring Pattern
        Expression init // nullable - can be null for uninitialized variables
        ) implements Node {

    public VariableDeclarator {
        if (id == null) throw new IllegalArgumentException("id required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "VariableDeclarator";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
