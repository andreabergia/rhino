/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.types;

/**
 * Represents a single-line comment.
 *
 * <p>Corresponds to JavaScript single-line comments that start with {@code //}. The value excludes
 * the {@code //} delimiter.
 *
 * <p>Example: For the comment {@code // hello world}, the value would be {@code hello world}.
 *
 * @param value Comment text without the {@code //} delimiter
 * @param start Absolute start offset in source
 * @param end Absolute end offset in source (exclusive)
 * @param loc Source location with line/column information, may be null
 */
public record CommentLine(String value, int start, int end, SourceLocation loc) implements Comment {

    /**
     * @throws IllegalArgumentException if value is null or positions are invalid
     */
    public CommentLine {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        if (start < 0) {
            throw new IllegalArgumentException("start must be >= 0, got: " + start);
        }
        if (end < start) {
            throw new IllegalArgumentException(
                    "end must be >= start, got start=" + start + ", end=" + end);
        }
    }

    @Override
    public String type() {
        return "CommentLine";
    }
}
