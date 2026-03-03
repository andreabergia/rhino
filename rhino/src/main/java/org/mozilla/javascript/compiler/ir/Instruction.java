/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import java.util.List;

/**
 * All instructions in the IR. Instructions with a {@code dest} register produce a value; those
 * without are side-effect only.
 */
public sealed interface Instruction {

    // --- SSA ---

    record PhiInput(BlockId predecessor, Register value) {}

    record Phi(Register dest, List<PhiInput> inputs) implements Instruction {}

    record Move(Register dest, Register src) implements Instruction {}

    // --- Source Info ---

    record Line(int lineNumber) implements Instruction {}

    record Debugger() implements Instruction {}

    // --- Constants ---

    record LoadConstant(Register dest, IrConstant value) implements Instruction {}

    record LoadTemplateLiteral(Register dest, int templateIndex) implements Instruction {}

    // --- Local Variable Access ---

    record GetVar(Register dest, int varIndex) implements Instruction {}

    record SetVar(int varIndex, Register src) implements Instruction {}

    record SetConstVar(int varIndex, Register src) implements Instruction {}

    // --- Scope Chain Access ---

    record GetName(Register dest, String name) implements Instruction {}

    record SetName(String name, Register scope, Register value) implements Instruction {}

    record StrictSetName(String name, Register scope, Register value) implements Instruction {}

    record SetConst(String name, Register scope, Register value) implements Instruction {}

    record BindName(Register dest, String name) implements Instruction {}

    record TypeOfName(Register dest, String name) implements Instruction {}

    record DeleteName(Register dest, String name) implements Instruction {}

    // --- Property Access ---

    record GetProp(Register dest, Register object, String name) implements Instruction {}

    record GetPropNoWarn(Register dest, Register object, String name) implements Instruction {}

    record GetPropSuper(Register dest, Register object, String name) implements Instruction {}

    record SetProp(Register dest, Register object, String name, Register value)
            implements Instruction {}

    record SetPropSuper(Register dest, Register object, String name, Register value)
            implements Instruction {}

    record GetElem(Register dest, Register object, Register index) implements Instruction {}

    record GetElemSuper(Register dest, Register object, Register index) implements Instruction {}

    record SetElem(Register dest, Register object, Register index, Register value)
            implements Instruction {}

    record SetElemSuper(Register dest, Register object, Register index, Register value)
            implements Instruction {}

    record DeleteProp(Register dest, Register object, Register index) implements Instruction {}

    record DeletePropSuper(Register dest) implements Instruction {}

    record GetPropOptional(Register dest, Register object, String name) implements Instruction {}

    record GetElemOptional(Register dest, Register object, Register index) implements Instruction {}

    // --- Operators ---

    record BinaryOp(Register dest, BinOp op, Register left, Register right)
            implements Instruction {}

    record UnaryOp(Register dest, UnOp op, Register operand) implements Instruction {}

    // --- Increment/Decrement ---

    record IncDecVar(Register dest, IncDecOp op, int varIndex) implements Instruction {}

    record IncDecName(Register dest, IncDecOp op, String name) implements Instruction {}

    record IncDecProp(Register dest, IncDecOp op, Register object, String name)
            implements Instruction {}

    record IncDecElem(Register dest, IncDecOp op, Register object, Register index)
            implements Instruction {}

    // --- Function/Method Resolution ---

    record NameAndThis(Register destFn, Register destThis, String name, boolean optional)
            implements Instruction {}

    record PropAndThis(
            Register destFn, Register destThis, Register object, String name, boolean optional)
            implements Instruction {}

    record ElemAndThis(
            Register destFn, Register destThis, Register object, Register index, boolean optional)
            implements Instruction {}

    record ValueAndThis(Register destFn, Register destThis, Register value, boolean optional)
            implements Instruction {}

    // --- Calls ---

    record Call(
            Register dest,
            Register function,
            Register thisObj,
            List<Register> args,
            CallKind callKind)
            implements Instruction {}

    record New(Register dest, Register constructor, List<Register> args) implements Instruction {}

    // --- Object/Array Literals ---

    record NewArray(Register dest, int size) implements Instruction {}

    record NewObject(Register dest) implements Instruction {}

    record InitProp(Register object, Object key, Register value, LiteralPropertyKind kind)
            implements Instruction {}

    record InitComputedProp(Register object, Register key, Register value, LiteralPropertyKind kind)
            implements Instruction {}

    record InitArrayElement(Register array, Register value) implements Instruction {}

    record ArraySpread(Register array, Register iterable) implements Instruction {}

    record ObjectSpread(Register object, Register source) implements Instruction {}

    record ObjectRest(
            Register dest,
            Register source,
            List<String> excludedKeys,
            List<Register> computedExcludedKeys)
            implements Instruction {}

    // --- Closures ---

    record CreateClosure(Register dest, int functionIndex, boolean isStatement, boolean isMethod)
            implements Instruction {}

    // --- Scope ---

    record EnterWith(Register object) implements Instruction {}

    record LeaveWith() implements Instruction {}

    record EnterCatch(Register dest, String name, int scopeIndex, Register exceptionObject)
            implements Instruction {}

    // --- Enumeration ---

    record EnumInit(Register dest, Register object, EnumKind kind) implements Instruction {}

    record EnumNext(Register dest, Register enumState) implements Instruction {}

    record EnumId(Register dest, Register enumState) implements Instruction {}

    // --- Special Values ---

    record GetThis(Register dest) implements Instruction {}

    record GetThisFn(Register dest) implements Instruction {}

    record GetSuper(Register dest) implements Instruction {}

    record GetNewTarget(Register dest) implements Instruction {}

    // --- Generators ---

    record Yield(Register dest, Register value) implements Instruction {}

    record YieldStar(Register dest, Register iterable) implements Instruction {}

    record GeneratorStart() implements Instruction {}

    record GeneratorReturn(Register value) implements Instruction {}

    record GeneratorEnd() implements Instruction {}

    // --- Ref Operations ---

    record GetRef(Register dest, Register ref) implements Instruction {}

    record SetRef(Register dest, Register ref, Register value) implements Instruction {}

    record DeleteRef(Register dest, Register ref) implements Instruction {}

    record RefSpecial(Register dest, Register object, String name) implements Instruction {}
}
