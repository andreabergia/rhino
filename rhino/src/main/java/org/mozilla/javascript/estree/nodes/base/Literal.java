/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.base;

import org.mozilla.javascript.estree.nodes.literals.RegExpLiteral;
import org.mozilla.javascript.estree.nodes.literals.SimpleLiteral;

/**
 * Base interface for all literal value nodes in the ESTree hierarchy.
 *
 * <p>Literals are primitive values that appear directly in the source code.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>Numbers: {@code 42}, {@code 3.14}
 *   <li>Strings: {@code "hello"}, {@code 'world'}
 *   <li>Booleans: {@code true}, {@code false}
 *   <li>Null: {@code null}
 *   <li>RegExp: {@code /pattern/flags}
 * </ul>
 *
 * <p>This interface is sealed and permits SimpleLiteral and RegExpLiteral implementations.
 */
public sealed interface Literal extends Expression permits SimpleLiteral, RegExpLiteral {
    /** Returns the literal value (String, Number, Boolean, or null). */
    Object value();

    /** Returns the raw string representation as it appears in source. */
    String raw();
}
