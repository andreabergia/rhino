/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.base;

import java.util.List;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Represents a literal value (string, number, boolean, null, etc.).
 *
 * <p>Literals are primitive values that appear directly in the source code.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>Numbers: {@code 42}, {@code 3.14}
 *   <li>Strings: {@code "hello"}, {@code 'world'}
 *   <li>Booleans: {@code true}, {@code false}
 *   <li>Null: {@code null}
 *   <li>RegExp: {@code /pattern/flags}
 * </ul>
 *
 * <p>This is a placeholder implementation for Phase 1. Full implementation with proper value types
 * will be added in Phase 2.
 *
 * @param loc Source location
 * @param start Start offset
 * @param end End offset
 * @param leadingComments Leading comments
 * @param trailingComments Trailing comments
 * @param innerComments Inner comments
 * @param value The literal value (String, Number, Boolean, or null)
 * @param raw The raw string representation as it appears in source
 */
public record Literal(
        SourceLocation loc,
        int start,
        int end,
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,
        Object value,
        String raw)
        implements Expression {

    public Literal {
        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    public Literal(SourceLocation loc, int start, int end, Object value, String raw) {
        this(loc, start, end, List.of(), List.of(), List.of(), value, raw);
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
