package org.mozilla.javascript.estree.nodes.expressions;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Member expression: {@code object.property} or {@code object[property]} Property access. Examples:
 * {@code obj.foo}, {@code obj["bar"]}, {@code arr[0]}
 */
public record MemberExpression(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Expression object,
        Expression property,
        boolean computed, // true for obj[prop], false for obj.prop
        boolean optional // true for obj?.prop (ES2020 optional chaining)
        ) implements Expression {

    public MemberExpression {
        if (object == null) throw new IllegalArgumentException("object required");
        if (property == null) throw new IllegalArgumentException("property required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    @Override
    public String type() {
        return "MemberExpression";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
