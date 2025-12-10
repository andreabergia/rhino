/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.literals;

import org.mozilla.javascript.estree.nodes.base.Literal;

/**
 * Base interface for simple literal nodes: string, number, boolean, null.
 *
 * <p>This interface is sealed and permits specialized implementations for type safety.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>String: {@code "hello"}, {@code 'world'}
 *   <li>Number: {@code 42}, {@code 3.14}
 *   <li>Boolean: {@code true}, {@code false}
 *   <li>Null: {@code null}
 * </ul>
 *
 * <p>Note: Unlike the ESTree spec which uses a single Literal type with Object value, this
 * implementation uses specialized subtypes for better type safety and pattern matching in Java.
 */
public sealed interface SimpleLiteral extends Literal
        permits StringLiteral, NumberLiteral, BooleanLiteral, NullLiteral {}
