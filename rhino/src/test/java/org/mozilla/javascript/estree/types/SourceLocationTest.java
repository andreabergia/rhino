/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class SourceLocationTest {

    @Test
    public void testValidSourceLocation() {
        Position start = new Position(1, 0);
        Position end = new Position(1, 10);
        SourceLocation loc = new SourceLocation(start, end, "test.js");

        assertEquals(start, loc.start());
        assertEquals(end, loc.end());
        assertEquals("test.js", loc.source());
    }

    @Test
    public void testSourceLocationWithoutSourceIdentifier() {
        Position start = new Position(1, 0);
        Position end = new Position(2, 5);
        SourceLocation loc = new SourceLocation(start, end);

        assertEquals(start, loc.start());
        assertEquals(end, loc.end());
        assertNull(loc.source());
    }

    @Test
    public void testSourceLocationWithNullSource() {
        Position start = new Position(1, 0);
        Position end = new Position(1, 10);
        SourceLocation loc = new SourceLocation(start, end, null);

        assertEquals(start, loc.start());
        assertEquals(end, loc.end());
        assertNull(loc.source());
    }

    @Test
    public void testStartCannotBeNull() {
        Position end = new Position(1, 10);
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new SourceLocation(null, end, "test.js"));
        assertEquals("start position cannot be null", exception.getMessage());
    }

    @Test
    public void testEndCannotBeNull() {
        Position start = new Position(1, 0);
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new SourceLocation(start, null, "test.js"));
        assertEquals("end position cannot be null", exception.getMessage());
    }

    @Test
    public void testMultiLineSourceLocation() {
        Position start = new Position(1, 0);
        Position end = new Position(5, 20);
        SourceLocation loc = new SourceLocation(start, end, "multi.js");

        assertEquals(1, loc.start().line());
        assertEquals(0, loc.start().column());
        assertEquals(5, loc.end().line());
        assertEquals(20, loc.end().column());
    }
}
