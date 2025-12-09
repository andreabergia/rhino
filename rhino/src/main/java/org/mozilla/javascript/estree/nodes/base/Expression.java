/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.base;

/**
 * Base interface for all expression nodes in the ESTree hierarchy.
 *
 * <p>Expressions are nodes that produce values and can appear in expression contexts. This includes
 * literals, identifiers, binary operations, function calls, and many other value-producing
 * constructs.
 *
 * <p>Examples of expressions:
 *
 * <ul>
 *   <li>Literals: {@code 42}, {@code "hello"}, {@code true}
 *   <li>Identifiers: {@code x}, {@code myVariable}
 *   <li>Binary operations: {@code x + y}, {@code a && b}
 *   <li>Function calls: {@code foo()}, {@code obj.method(arg)}
 *   <li>Object/Array literals: {@code {key: value}}, {@code [1, 2, 3]}
 * </ul>
 *
 * <p>This interface is sealed and will be extended to permit specific expression node types as they
 * are implemented in subsequent phases.
 */
public sealed interface Expression extends Node permits Literal, Identifier {
    // Sealed interface - concrete expression types will be added in later phases
}
