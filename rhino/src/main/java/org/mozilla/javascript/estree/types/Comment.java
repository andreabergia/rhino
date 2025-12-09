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
 * represent both single-line (//) and multi-line (/* *\/) comments in the source code.
 */
public sealed interface Comment permits CommentLine, CommentBlock {

    /**
     * Returns the comment type identifier.
     *
     * @return "CommentLine" or "CommentBlock"
     */
    String type();

    /**
     * Returns the comment text without delimiters.
     *
     * <p>For line comments, this excludes the leading "//". For block comments, this excludes the
     * opening "/*" and closing "*\/".
     *
     * @return The comment text content
     */
    String value();

    /**
     * Returns the absolute start offset in the source.
     *
     * @return Start byte offset (0-indexed)
     */
    int start();

    /**
     * Returns the absolute end offset in the source.
     *
     * @return End byte offset (exclusive)
     */
    int end();

    /**
     * Returns the source location with line and column information.
     *
     * @return Source location, or null if not available
     */
    SourceLocation loc();
}
