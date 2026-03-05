/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class IrValidatorTest {

    @Test
    void validNonSsaFunctionPasses() {
        CfgFunction fn = buildSimpleFunction();
        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(errors.isEmpty(), () -> "Expected no errors but got: " + errors);
    }

    @Test
    void validSsaFunctionPasses() {
        // Hand-craft a valid SSA function: two paths merge with a phi
        BasicBlock b0 =
                new BasicBlock(
                        new BlockId(0),
                        List.of(
                                new Instruction.LoadConstant(
                                        new Register(0), new IrConstant.BooleanConst(true))),
                        new Terminator.CondJump(new Register(0), new BlockId(1), new BlockId(2)));

        BasicBlock b1 =
                new BasicBlock(
                        new BlockId(1),
                        List.of(
                                new Instruction.LoadConstant(
                                        new Register(1), new IrConstant.NumberConst(1))),
                        new Terminator.Jump(new BlockId(3)));

        BasicBlock b2 =
                new BasicBlock(
                        new BlockId(2),
                        List.of(
                                new Instruction.LoadConstant(
                                        new Register(2), new IrConstant.NumberConst(2))),
                        new Terminator.Jump(new BlockId(3)));

        BasicBlock b3 =
                new BasicBlock(
                        new BlockId(3),
                        List.of(
                                new Instruction.Phi(
                                        new Register(3),
                                        List.of(
                                                new Instruction.PhiInput(
                                                        new BlockId(1), new Register(1)),
                                                new Instruction.PhiInput(
                                                        new BlockId(2), new Register(2))))),
                        new Terminator.Return(new Register(3)));

        CfgFunction fn =
                new CfgFunction(
                        "ssaTest",
                        "test.js",
                        1,
                        5,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(b0, b1, b2, b3),
                        List.of(),
                        List.of(),
                        4,
                        true);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.SSA);
        assertTrue(errors.isEmpty(), () -> "Expected no errors but got: " + errors);
    }

    @Test
    void missingEntryBlockDetected() {
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(99),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0), List.of(), new Terminator.ReturnVoid())),
                        List.of(),
                        List.of(),
                        0,
                        false);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Entry block")));
    }

    @Test
    void danglingBlockReferenceDetected() {
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(),
                                        new Terminator.Jump(new BlockId(99)))),
                        List.of(),
                        List.of(),
                        0,
                        false);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("non-existent block B99")));
    }

    @Test
    void registerOutOfRangeDetected() {
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(
                                                new Instruction.LoadConstant(
                                                        new Register(99),
                                                        new IrConstant.NumberConst(1))),
                                        new Terminator.ReturnVoid())),
                        List.of(),
                        List.of(),
                        1,
                        false);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("out of range")));
    }

    @Test
    void varIndexOutOfRangeDetected() {
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[] {"x"},
                        new boolean[] {false},
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(new Instruction.GetVar(new Register(0), 99)),
                                        new Terminator.ReturnVoid())),
                        List.of(),
                        List.of(),
                        1,
                        false);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("varIndex")));
    }

    @Test
    void varIndexZeroWithNoVariablesDetected() {
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(new Instruction.GetVar(new Register(0), 0)),
                                        new Terminator.ReturnVoid())),
                        List.of(),
                        List.of(),
                        1,
                        false);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(
                errors.stream().anyMatch(e -> e.contains("varIndex")),
                () -> "Expected varIndex error but got: " + errors);
    }

    @Test
    void nestedFunctionIndexOutOfRangeDetected() {
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(
                                                new Instruction.CreateClosure(
                                                        new Register(0), 99, false, false)),
                                        new Terminator.ReturnVoid())),
                        List.of(),
                        List.of(),
                        1,
                        false);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("functionIndex")));
    }

    @Test
    void undefinedRegisterUseDetected() {
        // r0 is used in return but never defined
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(),
                                        new Terminator.Return(new Register(0)))),
                        List.of(),
                        List.of(),
                        1,
                        false);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("never defined")));
    }

    @Test
    void invalidExceptionHandlerDetected() {
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0), List.of(), new Terminator.ReturnVoid())),
                        List.of(
                                new ExceptionHandler(
                                        List.of(new BlockId(0)), new BlockId(99), false)),
                        List.of(),
                        0,
                        false);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("handler") && e.contains("B99")));
    }

    @Test
    void paramCountExceedingVariableNamesDetected() {
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        5,
                        new String[] {"a"},
                        new boolean[] {false},
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0), List.of(), new Terminator.ReturnVoid())),
                        List.of(),
                        List.of(),
                        0,
                        false);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("paramCount")));
    }

    @Test
    void isConstLengthMismatchDetected() {
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[] {"a"},
                        new boolean[] {},
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0), List.of(), new Terminator.ReturnVoid())),
                        List.of(),
                        List.of(),
                        0,
                        false);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("isConst.length")));
    }

    @Test
    void ssaDuplicateRegisterDefinitionDetected() {
        // r0 defined twice
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[] {"x"},
                        new boolean[] {false},
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(
                                                new Instruction.GetVar(new Register(0), 0),
                                                new Instruction.GetVar(new Register(0), 0)),
                                        new Terminator.ReturnVoid())),
                        List.of(),
                        List.of(),
                        1,
                        true);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("defined in both")));
    }

    @Test
    void ssaPhiAfterNonPhiDetected() {
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(
                                                new Instruction.LoadConstant(
                                                        new Register(0),
                                                        new IrConstant.NumberConst(1))),
                                        new Terminator.Jump(new BlockId(1))),
                                new BasicBlock(
                                        new BlockId(1),
                                        List.of(
                                                new Instruction.LoadConstant(
                                                        new Register(1),
                                                        new IrConstant.NumberConst(2)),
                                                new Instruction.Phi(
                                                        new Register(2),
                                                        List.of(
                                                                new Instruction.PhiInput(
                                                                        new BlockId(0),
                                                                        new Register(0))))),
                                        new Terminator.Return(new Register(2)))),
                        List.of(),
                        List.of(),
                        3,
                        true);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Phi instruction after non-Phi")));
    }

    @Test
    void ssaPhiInputCountMismatchDetected() {
        // B1 has 1 predecessor (B0) but phi has 2 inputs
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(
                                                new Instruction.LoadConstant(
                                                        new Register(0),
                                                        new IrConstant.NumberConst(1)),
                                                new Instruction.LoadConstant(
                                                        new Register(1),
                                                        new IrConstant.NumberConst(2))),
                                        new Terminator.Jump(new BlockId(1))),
                                new BasicBlock(
                                        new BlockId(1),
                                        List.of(
                                                new Instruction.Phi(
                                                        new Register(2),
                                                        List.of(
                                                                new Instruction.PhiInput(
                                                                        new BlockId(0),
                                                                        new Register(0)),
                                                                new Instruction.PhiInput(
                                                                        new BlockId(1),
                                                                        new Register(1))))),
                                        new Terminator.Return(new Register(2)))),
                        List.of(),
                        List.of(),
                        3,
                        true);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.SSA);
        assertTrue(
                errors.stream().anyMatch(e -> e.contains("inputs") && e.contains("predecessors")));
    }

    @Test
    void ssaPhiWithWrongPredecessorBlockIdsDetected() {
        // B2 has predecessors B0 and B1, but phi says B0 and B99
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(
                                                new Instruction.LoadConstant(
                                                        new Register(0),
                                                        new IrConstant.NumberConst(1))),
                                        new Terminator.Jump(new BlockId(2))),
                                new BasicBlock(
                                        new BlockId(1),
                                        List.of(
                                                new Instruction.LoadConstant(
                                                        new Register(1),
                                                        new IrConstant.NumberConst(2))),
                                        new Terminator.Jump(new BlockId(2))),
                                new BasicBlock(
                                        new BlockId(2),
                                        List.of(
                                                new Instruction.Phi(
                                                        new Register(2),
                                                        List.of(
                                                                new Instruction.PhiInput(
                                                                        new BlockId(0),
                                                                        new Register(0)),
                                                                new Instruction.PhiInput(
                                                                        new BlockId(99),
                                                                        new Register(1))))),
                                        new Terminator.Return(new Register(2)))),
                        List.of(),
                        List.of(),
                        3,
                        true);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("wrong predecessor BlockIds")));
    }

    @Test
    void ssaPhiInEntryBlockDetected() {
        CfgFunction fn =
                new CfgFunction(
                        "bad",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(new Instruction.Phi(new Register(0), List.of())),
                                        new Terminator.ReturnVoid())),
                        List.of(),
                        List.of(),
                        1,
                        true);

        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.SSA);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Entry block") && e.contains("Phi")));
    }

    @Test
    void nestedFunctionErrorsDetected() {
        // Inner function with invalid entry block
        CfgFunction invalidInner =
                new CfgFunction(
                        "inner",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(99),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0), List.of(), new Terminator.ReturnVoid())),
                        List.of(),
                        List.of(),
                        0,
                        false);

        // Outer function is valid but contains the invalid inner function
        CfgFunction outer =
                new CfgFunction(
                        "outer",
                        "test.js",
                        1,
                        1,
                        0,
                        new String[0],
                        new boolean[0],
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        FunctionKind.STATEMENT,
                        new BlockId(0),
                        List.of(
                                new BasicBlock(
                                        new BlockId(0),
                                        List.of(
                                                new Instruction.CreateClosure(
                                                        new Register(0), 0, false, false)),
                                        new Terminator.ReturnVoid())),
                        List.of(),
                        List.of(invalidInner),
                        1,
                        false);

        List<String> errors = IrValidator.validate(outer, IrValidator.Mode.NON_SSA);
        assertTrue(
                errors.stream().anyMatch(e -> e.contains("nested[0]")),
                () -> "Expected nested function error but got: " + errors);
    }

    private static CfgFunction buildSimpleFunction() {
        CfgBuilder builder = new CfgBuilder("test", "test.js");
        builder.setParamCount(1);
        builder.setVariableNames(new String[] {"x"});
        builder.setIsConst(new boolean[] {false});
        builder.setBaseLineNumber(1);
        builder.setEndLineNumber(3);

        BlockId b0 = builder.newBlock();
        builder.setCurrentBlock(b0);
        Register r0 = builder.newRegister();
        builder.emit(new Instruction.GetVar(r0, 0));
        builder.terminate(new Terminator.Return(r0));

        return builder.build();
    }
}
