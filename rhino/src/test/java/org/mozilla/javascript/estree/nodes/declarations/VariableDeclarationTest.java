package org.mozilla.javascript.estree.nodes.declarations;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.estree.nodes.base.Identifier;
import org.mozilla.javascript.estree.nodes.literals.NumberLiteral;
import org.mozilla.javascript.estree.types.Position;
import org.mozilla.javascript.estree.types.SourceLocation;

class VariableDeclarationTest {

    @Test
    void testVarDeclaration() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 9), null);
        var id = new Identifier(loc, 4, 5, List.of(), List.of(), List.of(), "x");
        var init = new NumberLiteral(loc, 8, 9, List.of(), List.of(), List.of(), 1.0, "1");
        var declarator =
                new VariableDeclarator(loc, 4, 9, List.of(), List.of(), List.of(), id, init);

        var decl =
                new VariableDeclaration(
                        loc,
                        0,
                        9,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(declarator),
                        VariableDeclarationKind.VAR);

        assertEquals("VariableDeclaration", decl.type());
        assertEquals(VariableDeclarationKind.VAR, decl.kind());
        assertEquals(1, decl.declarations().size());
        assertEquals("x", ((Identifier) decl.declarations().get(0).id()).name());
    }

    @Test
    void testValidationNullKind() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 9), null);
        var id = new Identifier(loc, 4, 5, List.of(), List.of(), List.of(), "x");
        var declarator =
                new VariableDeclarator(loc, 4, 5, List.of(), List.of(), List.of(), id, null);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new VariableDeclaration(
                                loc,
                                0,
                                9,
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(declarator),
                                null));
    }

    @Test
    void testValidationEmptyDeclarations() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 9), null);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new VariableDeclaration(
                                loc,
                                0,
                                9,
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                VariableDeclarationKind.VAR));
    }

    @Test
    void testValidationNullDeclarations() {
        var loc = new SourceLocation(new Position(1, 0), new Position(1, 9), null);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new VariableDeclaration(
                                loc,
                                0,
                                9,
                                List.of(),
                                List.of(),
                                List.of(),
                                null,
                                VariableDeclarationKind.VAR));
    }
}
