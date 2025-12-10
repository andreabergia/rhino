package org.mozilla.javascript.estree.nodes.literals;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Literal;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Regular expression literal: {@code /pattern/flags} Represents a regular expression. Example:
 * {@code /[a-z]+/gi}
 */
public record RegExpLiteral(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Object value, // null for regex (cannot be represented as plain object)
        String raw, // Original source text representation
        RegExpValue regex // Pattern and flags
        ) implements Literal {

    public RegExpLiteral {
        if (regex == null) throw new IllegalArgumentException("regex required");
        if (regex.pattern == null) throw new IllegalArgumentException("regex.pattern required");
        if (regex.flags == null) throw new IllegalArgumentException("regex.flags required");

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

    /** Regular expression pattern and flags. */
    public record RegExpValue(String pattern, String flags) {}
}
