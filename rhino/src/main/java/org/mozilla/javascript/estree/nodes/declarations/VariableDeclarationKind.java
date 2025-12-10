/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.declarations;

/** Enum representing the kind of variable declaration. */
public enum VariableDeclarationKind {
    VAR("var"),
    LET("let"),
    CONST("const");

    private final String name;

    VariableDeclarationKind(String name) {
        this.name = name;
    }

    /** Returns the string representation ("var", "let", or "const") */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
