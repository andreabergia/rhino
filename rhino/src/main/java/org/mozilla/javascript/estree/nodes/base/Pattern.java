/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.base;

/**
 * Base interface for all pattern nodes in the ESTree hierarchy.
 *
 * <p>Patterns are nodes that appear on the left-hand side of assignments and in parameter lists.
 * They represent destructuring patterns and binding identifiers.
 *
 * <p>Examples of patterns:
 *
 * <ul>
 *   <li>Identifiers: {@code x} in {@code let x = 1;}
 *   <li>Array patterns: {@code [a, b, c]} in {@code let [a, b, c] = array;}
 *   <li>Object patterns: {@code {x, y}} in {@code let {x, y} = obj;}
 *   <li>Rest elements: {@code ...rest} in {@code let [first, ...rest] = array;}
 *   <li>Assignment patterns: {@code x = default} in {@code function f(x = 0) {}}
 * </ul>
 *
 * <p>Note that {@link Identifier} serves dual purpose as both an expression (when used to reference
 * a variable) and a pattern (when used to bind a variable).
 *
 * <p>This interface is sealed and will be extended to permit specific pattern node types as they
 * are implemented in subsequent phases.
 */
public sealed interface Pattern extends Node permits Identifier {}
