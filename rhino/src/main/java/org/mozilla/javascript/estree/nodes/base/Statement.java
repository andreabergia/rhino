/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.base;

/**
 * Base interface for all statement nodes in the ESTree hierarchy.
 *
 * <p>Statements are nodes that perform actions but don't produce values. They control program flow,
 * declare variables/functions, or wrap expressions.
 *
 * <p>Examples of statements:
 *
 * <ul>
 *   <li>Control flow: {@code if}, {@code while}, {@code for}, {@code switch}
 *   <li>Declarations: {@code var x = 1;}, {@code function foo() {}}
 *   <li>Flow control: {@code return}, {@code break}, {@code continue}, {@code throw}
 *   <li>Expression statements: {@code foo();}, {@code x = 1;}
 *   <li>Blocks: {@code { ... }}
 * </ul>
 *
 * <p>This interface is sealed and will be extended to permit specific statement node types as they
 * are implemented in subsequent phases. Note that {@link Declaration} extends Statement, as
 * declarations are a special category of statements.
 */
public sealed interface Statement extends Node permits Declaration {
    // Sealed interface - concrete statement types will be added in later phases
}
