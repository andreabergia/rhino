package org.mozilla.javascript.estree.nodes.properties;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Node;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Property in an object literal: {@code key: value} Represents a key-value pair in an object
 * expression. Examples: {@code {x: 1}}, {@code {name: "John"}}, {@code {[computed]: value}}
 */
public record Property(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Expression key, // Can be Identifier, Literal, or computed expression
        Expression value, // Can be any expression, or FunctionExpression for methods
        String kind, // "init", "get", or "set"
        boolean method, // true for method shorthand {foo() {}} (ES6+)
        boolean shorthand, // true for {x} shorthand (ES6+)
        boolean computed // true for computed property names {[expr]: value} (ES6+)
        ) implements Node {

    public Property {
        if (key == null) throw new IllegalArgumentException("key required");
        if (value == null) throw new IllegalArgumentException("value required");
        if (kind == null) throw new IllegalArgumentException("kind required");
        if (!kind.equals("init") && !kind.equals("get") && !kind.equals("set")) {
            throw new IllegalArgumentException("kind must be 'init', 'get', or 'set'");
        }

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "Property";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
