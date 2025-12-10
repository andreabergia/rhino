package org.mozilla.javascript.estree.nodes.clauses;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.estree.nodes.literals.SimpleLiteral;
import org.mozilla.javascript.estree.nodes.statements.EmptyStatement;
import org.mozilla.javascript.estree.types.Position;
import org.mozilla.javascript.estree.types.SourceLocation;

class SwitchCaseTest {

    @Test
    void testCaseClause() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 20), null);
        var test = new SimpleLiteral(loc, 5, 6, List.of(), List.of(), List.of(), 1.0, "1");
        var stmt = new EmptyStatement(loc, 8, 9, List.of(), List.of(), List.of());

        var switchCase =
                new SwitchCase(loc, 0, 20, List.of(), List.of(), List.of(), test, List.of(stmt));

        assertEquals("SwitchCase", switchCase.type());
        assertNotNull(switchCase.test());
        assertEquals(1, switchCase.consequent().size());
    }

    @Test
    void testDefaultClause() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 20), null);
        var stmt = new EmptyStatement(loc, 8, 9, List.of(), List.of(), List.of());

        var switchCase =
                new SwitchCase(loc, 0, 20, List.of(), List.of(), List.of(), null, List.of(stmt));

        assertNull(switchCase.test());
        assertEquals(1, switchCase.consequent().size());
    }

    @Test
    void testValidationNullConsequent() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 10), null);
        var test = new SimpleLiteral(loc, 5, 6, List.of(), List.of(), List.of(), 1.0, "1");

        assertThrows(
                IllegalArgumentException.class,
                () -> new SwitchCase(loc, 0, 10, List.of(), List.of(), List.of(), test, null));
    }
}
