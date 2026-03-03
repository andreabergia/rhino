/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import java.util.List;

/** Terminators end a basic block and define control flow to successor blocks. */
public sealed interface Terminator {

    record SwitchCase(Register caseValue, BlockId target) {}

    record Jump(BlockId target) implements Terminator {}

    record CondJump(Register condition, BlockId ifTrue, BlockId ifFalse) implements Terminator {}

    record CondJumpNullUndef(Register value, BlockId ifNullUndef, BlockId ifNotNullUndef)
            implements Terminator {}

    record Return(Register value) implements Terminator {}

    record ReturnVoid() implements Terminator {}

    record Throw(Register value, int lineNumber) implements Terminator {}

    record Rethrow(Register exceptionObject) implements Terminator {}

    record Switch(Register value, List<SwitchCase> cases, BlockId defaultTarget)
            implements Terminator {}

    record GoSub(BlockId target, BlockId continuation) implements Terminator {}
}
