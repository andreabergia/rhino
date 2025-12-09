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

public class CommentTest {

    @Test
    public void testCommentLine() {
        Position start = new Position(1, 0);
        Position end = new Position(1, 15);
        SourceLocation loc = new SourceLocation(start, end);
        CommentLine comment = new CommentLine(" hello world", 0, 15, loc);

        assertEquals("CommentLine", comment.type());
        assertEquals(" hello world", comment.value());
        assertEquals(0, comment.start());
        assertEquals(15, comment.end());
        assertEquals(loc, comment.loc());
    }

    @Test
    public void testCommentLineWithNullLocation() {
        CommentLine comment = new CommentLine(" test", 0, 7, null);

        assertEquals("CommentLine", comment.type());
        assertEquals(" test", comment.value());
        assertEquals(0, comment.start());
        assertEquals(7, comment.end());
        assertNull(comment.loc());
    }

    @Test
    public void testCommentLineValueCannotBeNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class, () -> new CommentLine(null, 0, 5, null));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    public void testCommentLineStartCannotBeNegative() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new CommentLine(" test", -1, 5, null));
        assertEquals("start must be >= 0, got: -1", exception.getMessage());
    }

    @Test
    public void testCommentLineEndMustBeGreaterThanOrEqualToStart() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new CommentLine(" test", 10, 5, null));
        assertEquals("end must be >= start, got start=10, end=5", exception.getMessage());
    }

    @Test
    public void testCommentBlock() {
        Position start = new Position(1, 0);
        Position end = new Position(3, 2);
        SourceLocation loc = new SourceLocation(start, end);
        CommentBlock comment = new CommentBlock(" multi\nline\ncomment ", 0, 25, loc);

        assertEquals("CommentBlock", comment.type());
        assertEquals(" multi\nline\ncomment ", comment.value());
        assertEquals(0, comment.start());
        assertEquals(25, comment.end());
        assertEquals(loc, comment.loc());
    }

    @Test
    public void testCommentBlockWithNullLocation() {
        CommentBlock comment = new CommentBlock(" test ", 0, 10, null);

        assertEquals("CommentBlock", comment.type());
        assertEquals(" test ", comment.value());
        assertEquals(0, comment.start());
        assertEquals(10, comment.end());
        assertNull(comment.loc());
    }

    @Test
    public void testCommentBlockValueCannotBeNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class, () -> new CommentBlock(null, 0, 5, null));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    public void testCommentBlockStartCannotBeNegative() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new CommentBlock(" test ", -1, 5, null));
        assertEquals("start must be >= 0, got: -1", exception.getMessage());
    }

    @Test
    public void testCommentBlockEndMustBeGreaterThanOrEqualToStart() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new CommentBlock(" test ", 10, 5, null));
        assertEquals("end must be >= start, got start=10, end=5", exception.getMessage());
    }

    @Test
    public void testCommentAsInterface() {
        Comment lineComment = new CommentLine(" line", 0, 7, null);
        Comment blockComment = new CommentBlock(" block ", 10, 21, null);

        assertEquals("CommentLine", lineComment.type());
        assertEquals("CommentBlock", blockComment.type());
    }

    @Test
    public void testEmptyCommentValue() {
        CommentLine emptyLine = new CommentLine("", 0, 2, null);
        CommentBlock emptyBlock = new CommentBlock("", 0, 4, null);

        assertEquals("", emptyLine.value());
        assertEquals("", emptyBlock.value());
    }
}
