/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.estree.nodes.base;

import org.mozilla.javascript.estree.nodes.statements.BlockStatement;
import org.mozilla.javascript.estree.nodes.statements.BreakStatement;
import org.mozilla.javascript.estree.nodes.statements.ContinueStatement;
import org.mozilla.javascript.estree.nodes.statements.DebuggerStatement;
import org.mozilla.javascript.estree.nodes.statements.DoWhileStatement;
import org.mozilla.javascript.estree.nodes.statements.EmptyStatement;
import org.mozilla.javascript.estree.nodes.statements.ExpressionStatement;
import org.mozilla.javascript.estree.nodes.statements.ForInStatement;
import org.mozilla.javascript.estree.nodes.statements.ForStatement;
import org.mozilla.javascript.estree.nodes.statements.IfStatement;
import org.mozilla.javascript.estree.nodes.statements.LabeledStatement;
import org.mozilla.javascript.estree.nodes.statements.ReturnStatement;
import org.mozilla.javascript.estree.nodes.statements.SwitchStatement;
import org.mozilla.javascript.estree.nodes.statements.ThrowStatement;
import org.mozilla.javascript.estree.nodes.statements.TryStatement;
import org.mozilla.javascript.estree.nodes.statements.WhileStatement;
import org.mozilla.javascript.estree.nodes.statements.WithStatement;

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
 * <p>This interface is sealed and permits all ES5 statement types. Note that {@link Declaration}
 * extends Statement, as declarations are a special category of statements.
 */
public sealed interface Statement extends Node
        permits Declaration,
                ExpressionStatement,
                BlockStatement,
                EmptyStatement,
                DebuggerStatement,
                WithStatement,
                ReturnStatement,
                LabeledStatement,
                BreakStatement,
                ContinueStatement,
                IfStatement,
                SwitchStatement,
                ThrowStatement,
                TryStatement,
                WhileStatement,
                DoWhileStatement,
                ForStatement,
                ForInStatement {}
