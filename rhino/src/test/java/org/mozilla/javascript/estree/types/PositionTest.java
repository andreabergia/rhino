/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class PositionTest {

    @Test
    public void testValidPosition() {
        Position pos = new Position(1, 0);
        assertEquals(1, pos.line());
        assertEquals(0, pos.column());
    }

    @Test
    public void testLineNumberMustBeAtLeastOne() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new Position(0, 0));
        assertEquals("line must be >= 1, got: 0", exception.getMessage());
    }

    @Test
    public void testLineNumberCannotBeNegative() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new Position(-1, 0));
        assertEquals("line must be >= 1, got: -1", exception.getMessage());
    }

    @Test
    public void testColumnNumberCannotBeNegative() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new Position(1, -1));
        assertEquals("column must be >= 0, got: -1", exception.getMessage());
    }
}
