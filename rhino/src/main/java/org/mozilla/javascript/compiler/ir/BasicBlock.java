/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import java.util.List;

/** A basic block: a sequence of instructions followed by a single terminator. */
public record BasicBlock(BlockId id, List<Instruction> instructions, Terminator terminator) {}
