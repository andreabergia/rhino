package org.mozilla.javascript.estree.nodes.declarations;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Declaration;
import org.mozilla.javascript.estree.nodes.base.Identifier;
import org.mozilla.javascript.estree.nodes.base.Pattern;
import org.mozilla.javascript.estree.nodes.statements.BlockStatement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Function declaration: {@code function id(params) body} Named function declaration (statement
 * form). Example: {@code function factorial(n) { return n <= 1 ? 1 : n * factorial(n-1); }}
 */
public record FunctionDeclaration(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Identifier
                id, // nullable in some contexts (export default function), but typically required
        List<Pattern> params,
        BlockStatement body,
        boolean generator, // true for function*
        boolean async // true for async function (ES2017+)
        ) implements Declaration {

    public FunctionDeclaration {
        if (params == null) throw new IllegalArgumentException("params required");
        if (body == null) throw new IllegalArgumentException("body required");

        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        params = List.copyOf(params);
    }

    @Override
    public String type() {
        return "FunctionDeclaration";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
