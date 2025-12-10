/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes;

import java.util.List;
import org.mozilla.javascript.estree.nodes.base.Node;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Represents a complete program (the root node of an ESTree AST).
 *
 * <p>The Program node is the top-level node of an ESTree abstract syntax tree. It contains a body
 * of statements that make up the program.
 *
 * <p>The sourceType indicates whether the program is a "script" (classic JavaScript) or "module"
 * (ES6 module with import/export).
 *
 * @param loc Source location
 * @param start Start offset
 * @param end End offset
 * @param leadingComments Leading comments (usually empty for Program)
 * @param trailingComments Trailing comments
 * @param innerComments Inner comments (comments in the program not attached to any statement)
 * @param sourceType Either "script" or "module"
 * @param body List of statements that make up the program
 */
public record Program(
        SourceLocation loc,
        int start,
        int end,
        List<Comment> leadingComments,
        List<Comment> trailingComments,
        List<Comment> innerComments,
        String sourceType,
        List<Statement> body)
        implements Node {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if required fields are invalid
     */
    public Program {
        if (sourceType == null || (!sourceType.equals("script") && !sourceType.equals("module"))) {
            throw new IllegalArgumentException(
                    "sourceType must be 'script' or 'module', got: " + sourceType);
        }
        if (body == null) {
            throw new IllegalArgumentException("body cannot be null");
        }

        // Make defensive copies of mutable collections
        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
        body = List.copyOf(body);
    }

    @Override
    public String type() {
        return "Program";
    }

    @Override
    public int[] range() {
        return new int[] {start, end};
    }
}
