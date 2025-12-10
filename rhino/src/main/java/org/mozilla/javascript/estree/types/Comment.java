/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.types;

/**
 * Base interface for comments in ESTree format.
 *
 * <p>Sealed to permit only {@link CommentLine} and {@link CommentBlock} implementations. Comments
 * represent both single-line ({@code //}) and multi-line ({@code /* *\/}) comments in the source
 * code.
 */
public sealed interface Comment permits CommentLine, CommentBlock {

    /** Returns the comment type identifier. */
    String type();

    /**
     * Returns the comment text without delimiters.
     *
     * <p>For line comments, this excludes the leading {@code //}. For block comments, this excludes
     * the opening {@code /*} and closing {@code *\/}.
     */
    String value();

    /** Returns the absolute start offset in the source. */
    int start();

    /** Returns the absolute end offset in the source. */
    int end();

    /** Returns the source location with line and column information. */
    SourceLocation loc();
}
