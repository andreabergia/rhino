/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.types;

/**
 * Source location with start and end positions.
 *
 * <p>Represents a range in source code with inclusive start position and exclusive end position.
 * Follows the ESTree specification for source location representation.
 *
 * @param start Starting position (inclusive)
 * @param end Ending position (exclusive)
 * @param source Optional source identifier (usually a filename), may be null
 */
public record SourceLocation(Position start, Position end, String source) {

    /**
     * @throws IllegalArgumentException if start or end is null
     */
    public SourceLocation {
        if (start == null) {
            throw new IllegalArgumentException("start position cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("end position cannot be null");
        }
    }

    /** Convenience constructor without source identifier. */
    public SourceLocation(Position start, Position end) {
        this(start, end, null);
    }
}
