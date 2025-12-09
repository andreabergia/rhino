/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.base;

/**
 * Base interface for all declaration nodes in the ESTree hierarchy.
 *
 * <p>Declarations are a special category of statements that introduce new bindings into a scope.
 * This includes variable, function, and class declarations.
 *
 * <p>Examples of declarations:
 *
 * <ul>
 *   <li>Variable declarations: {@code var x = 1;}, {@code let y = 2;}, {@code const z = 3;}
 *   <li>Function declarations: {@code function foo() {}}
 *   <li>Class declarations: {@code class MyClass {}}
 * </ul>
 *
 * <p>This interface is non-sealed for Phase 1. It will be made sealed to permit specific
 * declaration node types in subsequent phases when concrete declarations are implemented.
 */
public non-sealed interface Declaration extends Statement {
    // Non-sealed for Phase 1 - will be made sealed with concrete declaration types in later phases
}
