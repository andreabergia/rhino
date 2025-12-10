package org.mozilla.javascript.estree.nodes.statements;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.estree.nodes.base.Identifier;
import org.mozilla.javascript.estree.types.Position;
import org.mozilla.javascript.estree.types.SourceLocation;

class IfStatementTest {

    @Test
    void testIfElseStatement() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 30), null);
        var test = new Identifier(loc, 4, 5, List.of(), List.of(), List.of(), "x");
        var consequent =
                new ExpressionStatement(
                        loc,
                        7,
                        11,
                        List.of(),
                        List.of(),
                        List.of(),
                        new Identifier(loc, 7, 10, List.of(), List.of(), List.of(), "foo"));
        var alternate =
                new ExpressionStatement(
                        loc,
                        17,
                        21,
                        List.of(),
                        List.of(),
                        List.of(),
                        new Identifier(loc, 17, 20, List.of(), List.of(), List.of(), "bar"));

        var stmt =
                new IfStatement(
                        loc, 0, 30, List.of(), List.of(), List.of(), test, consequent, alternate);

        assertNotNull(stmt.alternate());
        assertEquals("ExpressionStatement", stmt.alternate().type());
    }

    @Test
    void testValidationNullTest() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 20), null);
        var consequent = new EmptyStatement(loc, 0, 1, List.of(), List.of(), List.of());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new IfStatement(
                                loc,
                                0,
                                20,
                                List.of(),
                                List.of(),
                                List.of(),
                                null,
                                consequent,
                                null));
    }

    @Test
    void testValidationNullConsequent() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 20), null);
        var test = new Identifier(loc, 4, 5, List.of(), List.of(), List.of(), "x");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new IfStatement(
                                loc, 0, 20, List.of(), List.of(), List.of(), test, null, null));
    }
}
