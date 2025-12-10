/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.estree.nodes.declarations.VariableDeclarationKind;

class VariableDeclarationKindTest {

    @Test
    void testGetName() {
        assertEquals("var", VariableDeclarationKind.VAR.getName());
        assertEquals("let", VariableDeclarationKind.LET.getName());
        assertEquals("const", VariableDeclarationKind.CONST.getName());
    }

    @Test
    void testToString() {
        assertEquals("var", VariableDeclarationKind.VAR.toString());
        assertEquals("let", VariableDeclarationKind.LET.toString());
        assertEquals("const", VariableDeclarationKind.CONST.toString());
    }
}
