/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.base;

import org.mozilla.javascript.estree.nodes.expressions.ArrayExpression;
import org.mozilla.javascript.estree.nodes.expressions.AssignmentExpression;
import org.mozilla.javascript.estree.nodes.expressions.BinaryExpression;
import org.mozilla.javascript.estree.nodes.expressions.CallExpression;
import org.mozilla.javascript.estree.nodes.expressions.ConditionalExpression;
import org.mozilla.javascript.estree.nodes.expressions.FunctionExpression;
import org.mozilla.javascript.estree.nodes.expressions.LogicalExpression;
import org.mozilla.javascript.estree.nodes.expressions.MemberExpression;
import org.mozilla.javascript.estree.nodes.expressions.NewExpression;
import org.mozilla.javascript.estree.nodes.expressions.ObjectExpression;
import org.mozilla.javascript.estree.nodes.expressions.SequenceExpression;
import org.mozilla.javascript.estree.nodes.expressions.ThisExpression;
import org.mozilla.javascript.estree.nodes.expressions.UnaryExpression;
import org.mozilla.javascript.estree.nodes.expressions.UpdateExpression;

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
 * <p>This interface is sealed and permits all ES5 expression node types.
 */
public sealed interface Expression extends Node
        permits Literal,
                Identifier,
                ThisExpression,
                ArrayExpression,
                ObjectExpression,
                FunctionExpression,
                UnaryExpression,
                UpdateExpression,
                BinaryExpression,
                AssignmentExpression,
                LogicalExpression,
                MemberExpression,
                ConditionalExpression,
                CallExpression,
                NewExpression,
                SequenceExpression {}
