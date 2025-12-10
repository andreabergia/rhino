package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.properties.Property;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Object expression: {@code {properties}} Object literal. Example: {@code {x: 1, y: 2}} or {@code
 * {name: "John", age: 30}}
 */
public record ObjectExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        List<Property> properties)
        implements Expression {

    public ObjectExpression {
        if (properties == null) throw new IllegalArgumentException("properties required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        properties = List.copyOf(properties);
    }

    @Override
    public String type() {
        return "ObjectExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
