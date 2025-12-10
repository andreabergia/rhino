/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.literals;

import java.util.List;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Boolean literal node.
 *
 * <p>Represents boolean literal values in source code.
 *
 * <p>Examples: {@code true}, {@code false}
 */
public record BooleanLiteral(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Boolean value, // The boolean value (boxed for SimpleLiteral compatibility)
        String raw // Original source text representation ("true" or "false")
        ) implements SimpleLiteral {

    public BooleanLiteral {
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

    /**
     * Returns the boolean value as a primitive boolean.
     *
     * @return the unboxed boolean value
     */
    public boolean booleanValue() {
        return value;
    }
}
