package org.mozilla.javascript.estree.adapter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.estree.types.Position;
import org.mozilla.javascript.estree.types.SourceLocation;

class PositionConverterTest {

    @Test
    void testSingleLinePositions() {
        String code = "var x = 1;";
        PositionConverter converter = new PositionConverter(code);

        // Start of line
        Position pos0 = converter.offsetToPosition(0);
        assertEquals(1, pos0.line());
        assertEquals(0, pos0.column());

        // Middle of line
        Position pos4 = converter.offsetToPosition(4);
        assertEquals(1, pos4.line());
        assertEquals(4, pos4.column());

        // End of line
        Position pos10 = converter.offsetToPosition(10);
        assertEquals(1, pos10.line());
        assertEquals(10, pos10.column());
    }

    @Test
    void testMultiLinePositions() {
        String code = "function foo() {\n  return 42;\n}";
        PositionConverter converter = new PositionConverter(code);

        // Line 1, column 0: "f"
        Position pos0 = converter.offsetToPosition(0);
        assertEquals(1, pos0.line());
        assertEquals(0, pos0.column());

        // Line 2, column 0: first space before "return"
        Position pos17 = converter.offsetToPosition(17);
        assertEquals(2, pos17.line());
        assertEquals(0, pos17.column());

        // Line 2, column 2: "r" in "return"
        Position pos19 = converter.offsetToPosition(19);
        assertEquals(2, pos19.line());
        assertEquals(2, pos19.column());

        // Line 3, column 0: "}"
        Position pos30 = converter.offsetToPosition(30);
        assertEquals(3, pos30.line());
        assertEquals(0, pos30.column());
    }

    @Test
    void testCreateLocation() {
        String code = "var x = 1;\nvar y = 2;";
        PositionConverter converter = new PositionConverter(code, "test.js");

        SourceLocation loc = converter.createLocation(0, 10);

        assertEquals(1, loc.start().line());
        assertEquals(0, loc.start().column());
        assertEquals(1, loc.end().line());
        assertEquals(10, loc.end().column());
        assertEquals("test.js", loc.source());
    }

    @Test
    void testSpanningMultipleLines() {
        String code = "function foo() {\n  return 42;\n}";
        PositionConverter converter = new PositionConverter(code);

        SourceLocation loc = converter.createLocation(0, code.length());

        assertEquals(1, loc.start().line());
        assertEquals(0, loc.start().column());
        assertEquals(3, loc.end().line());
        assertEquals(1, loc.end().column());
    }

    @Test
    void testEmptyLines() {
        String code = "var x = 1;\n\nvar y = 2;";
        PositionConverter converter = new PositionConverter(code);

        // Line 2 is empty, line 3 starts at offset 12
        Position pos12 = converter.offsetToPosition(12);
        assertEquals(3, pos12.line());
        assertEquals(0, pos12.column());
    }

    @Test
    void testOutOfBoundsThrows() {
        String code = "var x = 1;";
        PositionConverter converter = new PositionConverter(code);

        assertThrows(IllegalArgumentException.class, () -> converter.offsetToPosition(-1));
        assertThrows(IllegalArgumentException.class, () -> converter.offsetToPosition(11));
    }

    @Test
    void testLineCount() {
        assertEquals(1, new PositionConverter("single line").getLineCount());
        assertEquals(2, new PositionConverter("line 1\nline 2").getLineCount());
        assertEquals(3, new PositionConverter("line 1\nline 2\nline 3").getLineCount());
    }
}
