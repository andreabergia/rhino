/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.types;

/**
 * Position in source code.
 *
 * <p>Represents a single position in source code with line and column information. Line numbers are
 * 1-indexed (first line is 1), while column numbers are 0-indexed (first character is 0), per the
 * ESTree specification.
 *
 * @param line Line number, must be >= 1 (first line is 1)
 * @param column Column number, must be >= 0 (first character is 0)
 */
public record Position(int line, int column) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if line < 1 or column < 0
     */
    public Position {
        if (line < 1) {
            throw new IllegalArgumentException("line must be >= 1, got: " + line);
        }
        if (column < 0) {
            throw new IllegalArgumentException("column must be >= 0, got: " + column);
        }
    }
}
