package org.mozilla.javascript.estree.nodes.literals;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.estree.types.Position;
import org.mozilla.javascript.estree.types.SourceLocation;

class SimpleLiteralTest {

    @Test
    void testStringLiteral() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 7), null);
        var literal =
                new SimpleLiteral(loc, 0, 7, List.of(), List.of(), List.of(), "hello", "\"hello\"");

        assertEquals("Literal", literal.type());
        assertEquals("hello", literal.value());
        assertEquals("\"hello\"", literal.raw());
    }

    @Test
    void testNumberLiteral() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 2), null);
        var literal = new SimpleLiteral(loc, 0, 2, List.of(), List.of(), List.of(), 42.0, "42");

        assertEquals("Literal", literal.type());
        assertEquals(42.0, literal.value());
        assertEquals("42", literal.raw());
    }

    @Test
    void testBooleanLiteralTrue() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 4), null);
        var literal = new SimpleLiteral(loc, 0, 4, List.of(), List.of(), List.of(), true, "true");

        assertEquals("Literal", literal.type());
        assertEquals(true, literal.value());
        assertEquals("true", literal.raw());
    }

    @Test
    void testBooleanLiteralFalse() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 5), null);
        var literal = new SimpleLiteral(loc, 0, 5, List.of(), List.of(), List.of(), false, "false");

        assertEquals("Literal", literal.type());
        assertEquals(false, literal.value());
        assertEquals("false", literal.raw());
    }

    @Test
    void testNullLiteral() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 4), null);
        var literal = new SimpleLiteral(loc, 0, 4, List.of(), List.of(), List.of(), null, "null");

        assertEquals("Literal", literal.type());
        assertNull(literal.value());
        assertEquals("null", literal.raw());
    }

    @Test
    void testDecimalNumberLiteral() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 4), null);
        var literal = new SimpleLiteral(loc, 0, 4, List.of(), List.of(), List.of(), 3.14, "3.14");

        assertEquals(3.14, literal.value());
    }

    @Test
    void testValidationInvalidType() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 4), null);

        // Arrays are not valid literal values
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SimpleLiteral(
                                loc,
                                0,
                                4,
                                List.of(),
                                List.of(),
                                List.of(),
                                new int[] {1, 2, 3},
                                "[1,2,3]"));
    }

    @Test
    void testEscapedStringLiteral() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 10), null);
        var literal =
                new SimpleLiteral(
                        loc,
                        0,
                        10,
                        List.of(),
                        List.of(),
                        List.of(),
                        "line1\nline2",
                        "\"line1\\nline2\"");

        assertEquals("line1\nline2", literal.value());
        assertEquals("\"line1\\nline2\"", literal.raw());
    }
}
