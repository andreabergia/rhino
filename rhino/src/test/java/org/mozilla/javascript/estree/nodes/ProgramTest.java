/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.mozilla.javascript.estree.types.Position;
import org.mozilla.javascript.estree.types.SourceLocation;

public class ProgramTest {

    @Test
    public void testScriptProgram() {
        Position start = new Position(1, 0);
        Position end = new Position(1, 0);
        SourceLocation loc = new SourceLocation(start, end);

        Program program = new Program(loc, 0, 0, "script", List.of());

        assertEquals("Program", program.type());
        assertEquals("script", program.sourceType());
        assertEquals(0, program.body().size());
        assertEquals(loc, program.loc());
        assertEquals(0, program.start());
        assertEquals(0, program.end());
        assertArrayEquals(new int[] {0, 0}, program.range());
    }

    @Test
    public void testModuleProgram() {
        Position start = new Position(1, 0);
        Position end = new Position(1, 0);
        SourceLocation loc = new SourceLocation(start, end);

        Program program = new Program(loc, 0, 0, "module", List.of());

        assertEquals("Program", program.type());
        assertEquals("module", program.sourceType());
        assertEquals(0, program.body().size());
    }

    @Test
    public void testProgramWithComments() {
        Position start = new Position(1, 0);
        Position end = new Position(5, 10);
        SourceLocation loc = new SourceLocation(start, end);

        Program program =
                new Program(
                        loc, 0, 100, List.of(), // leading comments
                        List.of(), // trailing comments
                        List.of(), // inner comments
                        "script", List.of());

        assertTrue(program.leadingComments().isEmpty());
        assertTrue(program.trailingComments().isEmpty());
        assertTrue(program.innerComments().isEmpty());
    }

    @Test
    public void testConvenienceConstructor() {
        Position start = new Position(1, 0);
        Position end = new Position(1, 0);
        SourceLocation loc = new SourceLocation(start, end);

        Program program = new Program(loc, 0, 0, "script", List.of());

        assertEquals("Program", program.type());
        assertEquals("script", program.sourceType());
        assertTrue(program.leadingComments().isEmpty());
        assertTrue(program.trailingComments().isEmpty());
        assertTrue(program.innerComments().isEmpty());
    }

    @Test
    public void testInvalidSourceType() {
        Position start = new Position(1, 0);
        Position end = new Position(1, 0);
        SourceLocation loc = new SourceLocation(start, end);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new Program(loc, 0, 0, "invalid", List.of()));
        assertEquals(
                "sourceType must be 'script' or 'module', got: invalid", exception.getMessage());
    }

    @Test
    public void testNullSourceType() {
        Position start = new Position(1, 0);
        Position end = new Position(1, 0);
        SourceLocation loc = new SourceLocation(start, end);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new Program(loc, 0, 0, null, List.of()));
        assertEquals("sourceType must be 'script' or 'module', got: null", exception.getMessage());
    }

    @Test
    public void testNullBody() {
        Position start = new Position(1, 0);
        Position end = new Position(1, 0);
        SourceLocation loc = new SourceLocation(start, end);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new Program(loc, 0, 0, "script", null));
        assertEquals("body cannot be null", exception.getMessage());
    }

    @Test
    public void testRangeProperty() {
        Position start = new Position(1, 0);
        Position end = new Position(5, 20);
        SourceLocation loc = new SourceLocation(start, end);

        Program program = new Program(loc, 0, 100, "script", List.of());

        assertArrayEquals(new int[] {0, 100}, program.range());
    }
}
