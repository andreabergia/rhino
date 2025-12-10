/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.base;

import java.util.List;
import org.mozilla.javascript.estree.nodes.Program;
import org.mozilla.javascript.estree.nodes.clauses.CatchClause;
import org.mozilla.javascript.estree.nodes.clauses.SwitchCase;
import org.mozilla.javascript.estree.nodes.declarations.VariableDeclarator;
import org.mozilla.javascript.estree.nodes.properties.Property;
import org.mozilla.javascript.estree.types.Comment;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Root of the ESTree node hierarchy.
 *
 * <p>All ESTree nodes implement this interface. Provides common properties including:
 *
 * <ul>
 *   <li>Node type identification
 *   <li>Source location information (line/column and byte offsets)
 *   <li>Comment attachment (leading, trailing, and inner comments)
 * </ul>
 *
 * <p>This interface is sealed to ensure only valid ESTree node types can be created. The hierarchy
 * follows the ESTree specification with Expression, Statement, Declaration, Pattern, and other
 * specialized node types.
 */
public sealed interface Node
        permits Expression,
                Statement,
                Pattern,
                Program,
                VariableDeclarator,
                SwitchCase,
                CatchClause,
                Property {

    /**
     * Returns the node type identifier.
     *
     * <p>Each concrete node type returns a specific string matching the ESTree specification (e.g.,
     * "BinaryExpression", "IfStatement", "Program").
     *
     * @return The ESTree node type name
     */
    String type();

    /**
     * Returns the source location with line and column information.
     *
     * <p>This is the standard ESTree property containing start and end positions with line and
     * column numbers. May be null if location information is not available.
     *
     * @return Source location, or null if not available
     */
    SourceLocation loc();

    /**
     * Returns the absolute start offset in the source.
     *
     * <p>This is a common extension to ESTree for convenience, representing the byte offset from
     * the beginning of the source file (0-indexed).
     *
     * @return Start byte offset
     */
    int start();

    /**
     * Returns the absolute end offset in the source.
     *
     * <p>This is a common extension to ESTree for convenience, representing the byte offset from
     * the beginning of the source file (0-indexed, exclusive).
     *
     * @return End byte offset (exclusive)
     */
    int end();

    /**
     * Returns the byte offset range as an array.
     *
     * <p>This is a legacy property expected by some tools (ESLint, older parsers). Returns an array
     * of two integers: [start, end].
     *
     * @return Array of [start, end] byte offsets
     */
    int[] range();

    /**
     * Returns comments that appear before this node.
     *
     * <p>Leading comments are those that appear on lines before the node or on the same line before
     * the node starts.
     *
     * @return List of leading comments, empty if none (never null)
     */
    List<Comment> leadingComments();

    /**
     * Returns comments that appear after this node on the same line.
     *
     * <p>Trailing comments are those that appear on the same line as the node ends.
     *
     * @return List of trailing comments, empty if none (never null)
     */
    List<Comment> trailingComments();

    /**
     * Returns comments that appear inside this node.
     *
     * <p>Inner comments are those that appear within the node's boundaries but are not attached to
     * any child node. This is primarily used for block statements and other container nodes.
     *
     * @return List of inner comments, empty if none (never null)
     */
    List<Comment> innerComments();
}
