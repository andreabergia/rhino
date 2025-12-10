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
 * Number literal node.
 *
 * <p>Represents numeric literal values in source code.
 *
 * <p>Examples: {@code 42}, {@code 3.14}, {@code 0xFF}, {@code 1e10}
 */
public record NumberLiteral(
        // Position
        SourceLocation loc,
        int start,
        int end,

        // Comments
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,

        // Properties
        Double value, // The numeric value (boxed for SimpleLiteral compatibility)
        String raw // Original source text representation
        ) implements SimpleLiteral {

    public NumberLiteral {
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

    /** Returns the numeric value as a primitive double. */
    public double doubleValue() {
        return value;
    }
}
