/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class IrPrinterTest {

    @Test
    void printSimpleFunction() {
        CfgBuilder builder = new CfgBuilder("add", "test.js");
        builder.setParamCount(2);
        builder.setVariableNames(new String[] {"a", "b"});
        builder.setIsConst(new boolean[] {false, false});
        builder.setBaseLineNumber(1);
        builder.setEndLineNumber(3);

        BlockId b0 = builder.newBlock();
        builder.setCurrentBlock(b0);

        Register r0 = builder.newRegister();
        Register r1 = builder.newRegister();
        Register r2 = builder.newRegister();
        builder.emit(new Instruction.Line(2));
        builder.emit(new Instruction.GetVar(r0, 0));
        builder.emit(new Instruction.GetVar(r1, 1));
        builder.emit(new Instruction.BinaryOp(r2, BinOp.ADD, r0, r1));
        builder.terminate(new Terminator.Return(r2));

        CfgFunction fn = builder.build();
        String output = IrPrinter.print(fn);

        assertTrue(output.contains("function add(a, b)"));
        assertTrue(output.contains("test.js:1-3"));
        assertTrue(output.contains("strict=false"));
        assertTrue(output.contains("ssa=false"));
        assertTrue(output.contains("params=2"));
        assertTrue(output.contains("vars=[a, b]"));
        assertTrue(output.contains("registers=3"));
        assertTrue(output.contains("entry=B0"));
        assertTrue(output.contains("B0:"));
        assertTrue(output.contains("line 2"));
        assertTrue(output.contains("r0 = getvar 0"));
        assertTrue(output.contains("r1 = getvar 1"));
        assertTrue(output.contains("r2 = binop ADD r0, r1"));
        assertTrue(output.contains("return r2"));
    }

    @Test
    void printConditionalFunction() {
        CfgBuilder builder = new CfgBuilder("cond", "test.js");
        builder.setParamCount(1);
        builder.setVariableNames(new String[] {"x"});
        builder.setIsConst(new boolean[] {false});

        BlockId b0 = builder.newBlock();
        BlockId b1 = builder.newBlock();
        BlockId b2 = builder.newBlock();

        builder.setCurrentBlock(b0);
        Register cond = builder.newRegister();
        builder.emit(new Instruction.GetVar(cond, 0));
        builder.terminate(new Terminator.CondJump(cond, b1, b2));

        builder.setCurrentBlock(b1);
        Register trueVal = builder.newRegister();
        builder.emit(new Instruction.LoadConstant(trueVal, new IrConstant.NumberConst(1)));
        builder.terminate(new Terminator.Return(trueVal));

        builder.setCurrentBlock(b2);
        Register falseVal = builder.newRegister();
        builder.emit(new Instruction.LoadConstant(falseVal, new IrConstant.NumberConst(0)));
        builder.terminate(new Terminator.Return(falseVal));

        String output = IrPrinter.print(builder.build());

        assertTrue(output.contains("B0:"));
        assertTrue(output.contains("B1:"));
        assertTrue(output.contains("B2:"));
        assertTrue(output.contains("cond_jump r0 ? B1 : B2"));
    }

    @Test
    void printFunctionWithExceptionHandlers() {
        CfgBuilder builder = new CfgBuilder("tryCatch", "test.js");
        builder.setParamCount(0);
        builder.setVariableNames(new String[] {"e"});
        builder.setIsConst(new boolean[] {false});

        BlockId tryBlock = builder.newBlock();
        BlockId catchBlock = builder.newBlock();
        BlockId after = builder.newBlock();

        builder.setCurrentBlock(tryBlock);
        builder.terminate(new Terminator.Jump(after));

        builder.setCurrentBlock(catchBlock);
        Register exObj = builder.newRegister();
        Register catchScope = builder.newRegister();
        builder.emit(new Instruction.EnterCatch(catchScope, "e", 0, exObj));
        builder.terminate(new Terminator.Jump(after));

        builder.setCurrentBlock(after);
        builder.terminate(new Terminator.ReturnVoid());

        builder.addExceptionHandler(new ExceptionHandler(List.of(tryBlock), catchBlock, false));

        String output = IrPrinter.print(builder.build());

        assertTrue(output.contains("handler [B0] -> B1"));
        assertFalse(output.contains("(finally)"));
    }

    @Test
    void printPhiNodes() {
        // Hand-craft a CfgFunction with phi nodes to test printing
        BasicBlock b0 =
                new BasicBlock(
                        new BlockId(0),
                        List.of(
                                new Instruction.LoadConstant(
                                        new Register(0), new IrConstant.NumberConst(1))),
                        new Terminator.Jump(new BlockId(2)));

        BasicBlock b1 =
                new BasicBlock(
                        new BlockId(1),
                        List.of(
                                new Instruction.LoadConstant(
                                        new Register(1), new IrConstant.NumberConst(2))),
                        new Terminator.Jump(new BlockId(2)));

        BasicBlock b2 =
                new BasicBlock(
                        new BlockId(2),
                        List.of(
                                new Instruction.Phi(
                                        new Register(2),
                                        List.of(
                                                new Instruction.PhiInput(
                                                        new BlockId(0), new Register(0)),
                                                new Instruction.PhiInput(
                                                        new BlockId(1), new Register(1))))),
                        new Terminator.Return(new Register(2)));

        CfgFunction fn =
                new CfgFunction(
                        "phiTest",
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
                        List.of(b0, b1, b2),
                        List.of(),
                        List.of(),
                        3,
                        true);

        String output = IrPrinter.print(fn);
        assertTrue(output.contains("r2 = phi(B0: r0, B1: r1)"));
        assertTrue(output.contains("ssa=true"));
    }

    @Test
    void printAllInstructionFormats() {
        // Build a function with many instruction types to ensure no crashes
        CfgBuilder builder = new CfgBuilder("allFormats", "test.js");
        builder.setParamCount(1);
        builder.setVariableNames(new String[] {"x"});
        builder.setIsConst(new boolean[] {false});

        BlockId b0 = builder.newBlock();
        builder.setCurrentBlock(b0);

        Register r0 = builder.newRegister();
        Register r1 = builder.newRegister();
        Register r2 = builder.newRegister();
        Register r3 = builder.newRegister();
        Register r4 = builder.newRegister();
        Register r5 = builder.newRegister();
        Register r6 = builder.newRegister();
        Register r7 = builder.newRegister();
        Register r8 = builder.newRegister();

        builder.emit(new Instruction.Line(1));
        builder.emit(new Instruction.Debugger());
        builder.emit(new Instruction.LoadConstant(r0, new IrConstant.StringConst("hello")));
        builder.emit(new Instruction.LoadConstant(r1, new IrConstant.BooleanConst(true)));
        builder.emit(new Instruction.LoadConstant(r2, new IrConstant.NullConst()));
        builder.emit(new Instruction.LoadConstant(r3, new IrConstant.UndefinedConst()));
        builder.emit(new Instruction.LoadConstant(r4, new IrConstant.RegExpConst("abc", "gi")));
        builder.emit(new Instruction.GetVar(r5, 0));
        builder.emit(new Instruction.UnaryOp(r6, UnOp.TYPEOF, r5));
        builder.emit(new Instruction.GetThis(r7));
        builder.emit(new Instruction.NewObject(r8));
        builder.terminate(new Terminator.ReturnVoid());

        String output = IrPrinter.print(builder.build());

        assertTrue(output.contains("line 1"));
        assertTrue(output.contains("debugger"));
        assertTrue(output.contains("const \"hello\""));
        assertTrue(output.contains("const true"));
        assertTrue(output.contains("const null"));
        assertTrue(output.contains("const undefined"));
        assertTrue(output.contains("const /abc/gi"));
        assertTrue(output.contains("unop TYPEOF"));
        assertTrue(output.contains("get_this"));
        assertTrue(output.contains("new_object"));
        assertTrue(output.contains("return_void"));
    }
}
