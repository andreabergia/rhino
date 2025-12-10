package org.mozilla.javascript.estree.nodes.expressions;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.estree.nodes.base.Identifier;
import org.mozilla.javascript.estree.types.BinaryOperator;
import org.mozilla.javascript.estree.types.Position;
import org.mozilla.javascript.estree.types.SourceLocation;

class BinaryExpressionTest {

    @Test
    void testBasicConstruction() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 5), null);
        var left = new Identifier(loc, 0, 1, List.of(), List.of(), List.of(), "x");
        var right = new Identifier(loc, 4, 5, List.of(), List.of(), List.of(), "y");

        var expr =
                new BinaryExpression(
                        loc,
                        0,
                        5,
                        List.of(),
                        List.of(),
                        List.of(),
                        BinaryOperator.ADD,
                        left,
                        right);

        assertEquals("BinaryExpression", expr.type());
        assertEquals(BinaryOperator.ADD, expr.operator());
        assertEquals("+", expr.operator().toString());
        assertEquals("x", ((Identifier) expr.left()).name());
        assertEquals("y", ((Identifier) expr.right()).name());
        assertEquals(0, expr.start());
        assertEquals(5, expr.end());
        assertArrayEquals(new int[] {0, 5}, expr.range());
    }

    @Test
    void testValidationNullOperator() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 5), null);
        var left = new Identifier(loc, 0, 1, List.of(), List.of(), List.of(), "x");
        var right = new Identifier(loc, 4, 5, List.of(), List.of(), List.of(), "y");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new BinaryExpression(
                                loc, 0, 5, List.of(), List.of(), List.of(), null, left, right));
    }

    @Test
    void testValidationNullLeft() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 5), null);
        var right = new Identifier(loc, 4, 5, List.of(), List.of(), List.of(), "y");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new BinaryExpression(
                                loc,
                                0,
                                5,
                                List.of(),
                                List.of(),
                                List.of(),
                                BinaryOperator.ADD,
                                null,
                                right));
    }

    @Test
    void testValidationNullRight() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 5), null);
        var left = new Identifier(loc, 0, 1, List.of(), List.of(), List.of(), "x");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new BinaryExpression(
                                loc,
                                0,
                                5,
                                List.of(),
                                List.of(),
                                List.of(),
                                BinaryOperator.ADD,
                                left,
                                null));
    }
}
