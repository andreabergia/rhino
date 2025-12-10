package org.mozilla.javascript.estree.nodes.expressions;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.estree.nodes.base.Identifier;
import org.mozilla.javascript.estree.types.Position;
import org.mozilla.javascript.estree.types.SourceLocation;

class CallExpressionTest {

    @Test
    void testBasicCall() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 7), null);
        var callee = new Identifier(loc, 0, 3, List.of(), List.of(), List.of(), "foo");

        var expr =
                new CallExpression(
                        loc, 0, 7, List.of(), List.of(), List.of(), callee, List.of(), false);

        assertEquals("CallExpression", expr.type());
        assertEquals("foo", ((Identifier) expr.callee()).name());
        assertTrue(expr.arguments().isEmpty());
        assertFalse(expr.optional());
    }

    @Test
    void testValidationNullCallee() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 7), null);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new CallExpression(
                                loc, 0, 7, List.of(), List.of(), List.of(), null, List.of(),
                                false));
    }

    @Test
    void testValidationNullArguments() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 7), null);
        var callee = new Identifier(loc, 0, 3, List.of(), List.of(), List.of(), "foo");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new CallExpression(
                                loc, 0, 7, List.of(), List.of(), List.of(), callee, null, false));
    }
}
