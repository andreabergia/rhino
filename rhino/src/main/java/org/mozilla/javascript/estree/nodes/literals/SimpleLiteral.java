package org.mozilla.javascript.estree.nodes.literals;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Literal;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Simple literal: string, number, boolean, null Represents primitive literal values. Examples:
 * {@code "hello"}, {@code 42}, {@code 3.14}, {@code true}, {@code false}, {@code null}
 */
public record SimpleLiteral(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Object value, // String, Number (Double), Boolean, or null
        String raw // Original source text representation
        ) implements Literal {

    public SimpleLiteral {
        // Validate that value is one of the allowed types
        if (value != null
                && !(value instanceof String)
                && !(value instanceof Number)
                && !(value instanceof Boolean)) {
            throw new IllegalArgumentException("value must be String, Number, Boolean, or null");
        }

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "Literal";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
