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
 * Represents an identifier (variable name, property name, etc.).
 *
 * <p>Identifiers serve dual purpose in ESTree:
 *
 * <ul>
 *   <li>As expressions: when referencing a variable (e.g., {@code x} in {@code x + 1})
 *   <li>As patterns: when binding a variable (e.g., {@code x} in {@code let x = 1})
 * </ul>
 *
 * <p>This is a placeholder implementation for Phase 1. Full implementation will be added in Phase
 * 2.
 *
 * @param loc Source location
 * @param start Start offset
 * @param end End offset
 * @param leadingComments Leading comments
 * @param trailingComments Trailing comments
 * @param innerComments Inner comments
 * @param name The identifier name
 */
public record Identifier(
        SourceLocation loc,
        int start,
        int end,
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,
        String name)
        implements Expression, Pattern {

    public Identifier {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    public Identifier(SourceLocation loc, int start, int end, String name) {
        this(loc, start, end, List.of(), List.of(), List.of(), name);
    }

    @Override
    public String type() {
        return "Identifier";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
