/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CfgBuilderTest {

    @Test
    void buildSimpleFunction() {
        CfgBuilder builder = new CfgBuilder("simple", "test.js");
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

        CfgFunction fn = builder.build();
        assertEquals("simple", fn.name());
        assertEquals("test.js", fn.sourceName());
        assertEquals(1, fn.paramCount());
        assertEquals(1, fn.blocks().size());
        assertEquals(b0, fn.entryBlock());
        assertEquals(1, fn.registerCount());
        assertFalse(fn.isSsa());

        BasicBlock block = fn.blocks().get(0);
        assertEquals(1, block.instructions().size());
        assertInstanceOf(Instruction.GetVar.class, block.instructions().get(0));
        assertInstanceOf(Terminator.Return.class, block.terminator());
    }

    @Test
    void buildBinaryOpFunction() {
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
        builder.emit(new Instruction.GetVar(r0, 0));
        builder.emit(new Instruction.GetVar(r1, 1));
        builder.emit(new Instruction.BinaryOp(r2, BinOp.ADD, r0, r1));
        builder.terminate(new Terminator.Return(r2));

        CfgFunction fn = builder.build();
        assertEquals(3, fn.registerCount());
        assertEquals(1, fn.blocks().size());
        assertEquals(3, fn.blocks().get(0).instructions().size());
    }

    @Test
    void buildConditional() {
        CfgBuilder builder = new CfgBuilder("cond", "test.js");
        builder.setParamCount(1);
        builder.setVariableNames(new String[] {"x"});
        builder.setIsConst(new boolean[] {false});

        BlockId entry = builder.newBlock();
        BlockId thenBlock = builder.newBlock();
        BlockId elseBlock = builder.newBlock();

        builder.setCurrentBlock(entry);
        Register cond = builder.newRegister();
        builder.emit(new Instruction.GetVar(cond, 0));
        builder.terminate(new Terminator.CondJump(cond, thenBlock, elseBlock));

        Register trueVal = builder.newRegister();
        builder.setCurrentBlock(thenBlock);
        builder.emit(new Instruction.LoadConstant(trueVal, new IrConstant.NumberConst(1)));
        builder.terminate(new Terminator.Return(trueVal));

        Register falseVal = builder.newRegister();
        builder.setCurrentBlock(elseBlock);
        builder.emit(new Instruction.LoadConstant(falseVal, new IrConstant.NumberConst(0)));
        builder.terminate(new Terminator.Return(falseVal));

        CfgFunction fn = builder.build();
        assertEquals(3, fn.blocks().size());
    }

    @Test
    void cfgAnalysisDiamondCfg() {
        CfgBuilder builder = new CfgBuilder("diamond", "test.js");
        builder.setParamCount(1);
        builder.setVariableNames(new String[] {"x"});
        builder.setIsConst(new boolean[] {false});

        BlockId entry = builder.newBlock();
        BlockId left = builder.newBlock();
        BlockId right = builder.newBlock();
        BlockId merge = builder.newBlock();

        builder.setCurrentBlock(entry);
        Register cond = builder.newRegister();
        builder.emit(new Instruction.GetVar(cond, 0));
        builder.terminate(new Terminator.CondJump(cond, left, right));

        builder.setCurrentBlock(left);
        builder.terminate(new Terminator.Jump(merge));

        builder.setCurrentBlock(right);
        builder.terminate(new Terminator.Jump(merge));

        builder.setCurrentBlock(merge);
        builder.terminate(new Terminator.ReturnVoid());

        CfgFunction fn = builder.build();
        CfgAnalysis analysis = CfgAnalysis.compute(fn);

        // Predecessors
        assertTrue(analysis.getPredecessors(entry).isEmpty());
        assertEquals(List.of(entry), analysis.getPredecessors(left));
        assertEquals(List.of(entry), analysis.getPredecessors(right));
        assertEquals(2, analysis.getPredecessors(merge).size());
        assertTrue(analysis.getPredecessors(merge).contains(left));
        assertTrue(analysis.getPredecessors(merge).contains(right));

        // Successors
        assertEquals(2, analysis.getSuccessors(entry).size());
        assertTrue(analysis.getSuccessors(entry).contains(left));
        assertTrue(analysis.getSuccessors(entry).contains(right));
        assertEquals(List.of(merge), analysis.getSuccessors(left));
        assertEquals(List.of(merge), analysis.getSuccessors(right));
        assertTrue(analysis.getSuccessors(merge).isEmpty());

        // Reverse postorder: entry before left/right, left/right before merge
        List<BlockId> rpo = analysis.getReversePostOrder();
        assertEquals(4, rpo.size());
        assertEquals(entry, rpo.get(0));
        assertTrue(rpo.indexOf(left) < rpo.indexOf(merge));
        assertTrue(rpo.indexOf(right) < rpo.indexOf(merge));
    }

    @Test
    void buildLoop() {
        CfgBuilder builder = new CfgBuilder("loop", "test.js");
        builder.setParamCount(0);
        builder.setVariableNames(new String[] {"i"});
        builder.setIsConst(new boolean[] {false});

        BlockId init = builder.newBlock();
        BlockId header = builder.newBlock();
        BlockId body = builder.newBlock();
        BlockId exit = builder.newBlock();

        builder.setCurrentBlock(init);
        Register zero = builder.newRegister();
        builder.emit(new Instruction.LoadConstant(zero, new IrConstant.NumberConst(0)));
        builder.emit(new Instruction.SetVar(0, zero));
        builder.terminate(new Terminator.Jump(header));

        builder.setCurrentBlock(header);
        Register i = builder.newRegister();
        Register limit = builder.newRegister();
        Register cond = builder.newRegister();
        builder.emit(new Instruction.GetVar(i, 0));
        builder.emit(new Instruction.LoadConstant(limit, new IrConstant.NumberConst(10)));
        builder.emit(new Instruction.BinaryOp(cond, BinOp.LT, i, limit));
        builder.terminate(new Terminator.CondJump(cond, body, exit));

        builder.setCurrentBlock(body);
        Register inc = builder.newRegister();
        builder.emit(new Instruction.IncDecVar(inc, IncDecOp.POST_INC, 0));
        builder.terminate(new Terminator.Jump(header));

        builder.setCurrentBlock(exit);
        builder.terminate(new Terminator.ReturnVoid());

        CfgFunction fn = builder.build();
        assertEquals(4, fn.blocks().size());
    }

    @Test
    void buildWithNestedFunction() {
        // Build inner function first
        CfgBuilder innerBuilder = new CfgBuilder("inner", "test.js");
        innerBuilder.setParamCount(0);
        innerBuilder.setVariableNames(new String[0]);
        innerBuilder.setIsConst(new boolean[0]);
        BlockId innerBlock = innerBuilder.newBlock();
        innerBuilder.setCurrentBlock(innerBlock);
        innerBuilder.terminate(new Terminator.ReturnVoid());
        CfgFunction innerFn = innerBuilder.build();

        // Build outer function
        CfgBuilder builder = new CfgBuilder("outer", "test.js");
        builder.setParamCount(0);
        builder.setVariableNames(new String[0]);
        builder.setIsConst(new boolean[0]);
        int fnIndex = builder.addNestedFunction(innerFn);

        BlockId b0 = builder.newBlock();
        builder.setCurrentBlock(b0);
        Register r0 = builder.newRegister();
        builder.emit(new Instruction.CreateClosure(r0, fnIndex, false, false));
        builder.terminate(new Terminator.ReturnVoid());

        CfgFunction fn = builder.build();
        assertEquals(1, fn.nestedFunctions().size());
        assertEquals("inner", fn.nestedFunctions().get(0).name());
    }

    @Test
    void buildWithExceptionHandler() {
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

        CfgFunction fn = builder.build();
        assertEquals(1, fn.exceptionHandlers().size());
        assertEquals(catchBlock, fn.exceptionHandlers().get(0).handlerBlock());
    }

    @Test
    void buildSwitch() {
        CfgBuilder builder = new CfgBuilder("switchFn", "test.js");
        builder.setParamCount(1);
        builder.setVariableNames(new String[] {"x"});
        builder.setIsConst(new boolean[] {false});

        BlockId entry = builder.newBlock();
        BlockId case1 = builder.newBlock();
        BlockId case2 = builder.newBlock();
        BlockId defaultBlock = builder.newBlock();

        builder.setCurrentBlock(entry);
        Register val = builder.newRegister();
        Register c1 = builder.newRegister();
        Register c2 = builder.newRegister();
        builder.emit(new Instruction.GetVar(val, 0));
        builder.emit(new Instruction.LoadConstant(c1, new IrConstant.NumberConst(1)));
        builder.emit(new Instruction.LoadConstant(c2, new IrConstant.NumberConst(2)));
        builder.terminate(
                new Terminator.Switch(
                        val,
                        List.of(
                                new Terminator.SwitchCase(c1, case1),
                                new Terminator.SwitchCase(c2, case2)),
                        defaultBlock));

        builder.setCurrentBlock(case1);
        builder.terminate(new Terminator.ReturnVoid());
        builder.setCurrentBlock(case2);
        builder.terminate(new Terminator.ReturnVoid());
        builder.setCurrentBlock(defaultBlock);
        builder.terminate(new Terminator.ReturnVoid());

        CfgFunction fn = builder.build();
        assertEquals(4, fn.blocks().size());
        assertInstanceOf(Terminator.Switch.class, fn.blocks().get(0).terminator());
    }

    @Test
    void buildFailsWithoutTerminator() {
        CfgBuilder builder = new CfgBuilder("bad", "test.js");
        builder.setParamCount(0);
        builder.setVariableNames(new String[0]);
        builder.setIsConst(new boolean[0]);

        builder.newBlock();
        builder.setCurrentBlock(new BlockId(0));

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void builtFunctionHasImmutableLists() {
        CfgBuilder builder = new CfgBuilder("immut", "test.js");
        builder.setParamCount(0);
        builder.setVariableNames(new String[0]);
        builder.setIsConst(new boolean[0]);
        BlockId b0 = builder.newBlock();
        builder.setCurrentBlock(b0);
        builder.terminate(new Terminator.ReturnVoid());

        CfgFunction fn = builder.build();
        assertThrows(UnsupportedOperationException.class, () -> fn.blocks().add(null));
        assertThrows(UnsupportedOperationException.class, () -> fn.exceptionHandlers().add(null));
        assertThrows(UnsupportedOperationException.class, () -> fn.nestedFunctions().add(null));
        assertThrows(
                UnsupportedOperationException.class,
                () -> fn.blocks().get(0).instructions().add(null));
    }

    @Test
    void allInstructionVariantsCanBeConstructed() {
        Register r0 = new Register(0);
        Register r1 = new Register(1);
        Register r2 = new Register(2);
        BlockId b0 = new BlockId(0);
        BlockId b1 = new BlockId(1);

        // SSA
        assertNotNull(
                new Instruction.Phi(
                        r0,
                        List.of(
                                new Instruction.PhiInput(b0, r1),
                                new Instruction.PhiInput(b1, r2))));
        assertNotNull(new Instruction.Move(r0, r1));

        // Source Info
        assertNotNull(new Instruction.Line(42));
        assertNotNull(new Instruction.Debugger());

        // Constants
        assertNotNull(new Instruction.LoadConstant(r0, new IrConstant.NumberConst(3.14)));
        assertNotNull(new Instruction.LoadTemplateLiteral(r0, 0));

        // Local Variable Access
        assertNotNull(new Instruction.GetVar(r0, 0));
        assertNotNull(new Instruction.SetVar(0, r0));
        assertNotNull(new Instruction.SetConstVar(0, r0));

        // Scope Chain Access
        assertNotNull(new Instruction.GetName(r0, "x"));
        assertNotNull(new Instruction.SetName("x", r0, r1));
        assertNotNull(new Instruction.StrictSetName("x", r0, r1));
        assertNotNull(new Instruction.SetConst("x", r0, r1));
        assertNotNull(new Instruction.BindName(r0, "x"));
        assertNotNull(new Instruction.TypeOfName(r0, "x"));
        assertNotNull(new Instruction.DeleteName(r0, "x"));

        // Property Access
        assertNotNull(new Instruction.GetProp(r0, r1, "p"));
        assertNotNull(new Instruction.GetPropNoWarn(r0, r1, "p"));
        assertNotNull(new Instruction.GetPropSuper(r0, r1, "p"));
        assertNotNull(new Instruction.SetProp(r0, r1, "p", r2));
        assertNotNull(new Instruction.SetPropSuper(r0, r1, "p", r2));
        assertNotNull(new Instruction.GetElem(r0, r1, r2));
        assertNotNull(new Instruction.GetElemSuper(r0, r1, r2));
        assertNotNull(new Instruction.SetElem(r0, r1, r2, new Register(3)));
        assertNotNull(new Instruction.SetElemSuper(r0, r1, r2, new Register(3)));
        assertNotNull(new Instruction.DeleteProp(r0, r1, r2));
        assertNotNull(new Instruction.DeletePropSuper(r0));
        assertNotNull(new Instruction.GetPropOptional(r0, r1, "p"));
        assertNotNull(new Instruction.GetElemOptional(r0, r1, r2));

        // Operators
        assertNotNull(new Instruction.BinaryOp(r0, BinOp.ADD, r1, r2));
        assertNotNull(new Instruction.UnaryOp(r0, UnOp.NEG, r1));

        // Increment/Decrement
        assertNotNull(new Instruction.IncDecVar(r0, IncDecOp.PRE_INC, 0));
        assertNotNull(new Instruction.IncDecName(r0, IncDecOp.POST_DEC, "x"));
        assertNotNull(new Instruction.IncDecProp(r0, IncDecOp.PRE_INC, r1, "p"));
        assertNotNull(new Instruction.IncDecElem(r0, IncDecOp.POST_INC, r1, r2));

        // Function/Method Resolution
        assertNotNull(new Instruction.NameAndThis(r0, r1, "fn", false));
        assertNotNull(new Instruction.PropAndThis(r0, r1, r2, "fn", true));
        assertNotNull(new Instruction.ElemAndThis(r0, r1, r2, new Register(3), false));
        assertNotNull(new Instruction.ValueAndThis(r0, r1, r2, false));

        // Calls
        assertNotNull(new Instruction.Call(r0, r1, r2, List.of(), CallKind.NORMAL));
        assertNotNull(new Instruction.New(r0, r1, List.of(r2)));

        // Object/Array Literals
        assertNotNull(new Instruction.NewArray(r0, 10));
        assertNotNull(new Instruction.NewObject(r0));
        assertNotNull(
                new Instruction.InitProp(
                        r0, new IrConstant.StringConst("key"), r1, LiteralPropertyKind.VALUE));
        assertNotNull(new Instruction.InitComputedProp(r0, r1, r2, LiteralPropertyKind.GETTER));
        assertNotNull(new Instruction.InitArrayElement(r0, r1));
        assertNotNull(new Instruction.ArraySpread(r0, r1));
        assertNotNull(new Instruction.ObjectSpread(r0, r1));
        assertNotNull(new Instruction.ObjectRest(r0, r1, List.of("a", "b"), List.of(r2)));

        // Closures
        assertNotNull(new Instruction.CreateClosure(r0, 0, true, false));

        // Scope
        assertNotNull(new Instruction.EnterWith(r0));
        assertNotNull(new Instruction.LeaveWith());
        assertNotNull(new Instruction.EnterCatch(r0, "e", 0, r1));

        // Enumeration
        assertNotNull(new Instruction.EnumInit(r0, r1, EnumKind.KEYS));
        assertNotNull(new Instruction.EnumNext(r0, r1));
        assertNotNull(new Instruction.EnumId(r0, r1));

        // Special Values
        assertNotNull(new Instruction.GetThis(r0));
        assertNotNull(new Instruction.GetThisFn(r0));
        assertNotNull(new Instruction.GetSuper(r0));
        assertNotNull(new Instruction.GetNewTarget(r0));

        // Generators
        assertNotNull(new Instruction.Yield(r0, r1));
        assertNotNull(new Instruction.YieldStar(r0, r1));
        assertNotNull(new Instruction.GeneratorStart());
        assertNotNull(new Instruction.GeneratorReturn(r0));
        assertNotNull(new Instruction.GeneratorEnd());

        // Ref Operations
        assertNotNull(new Instruction.GetRef(r0, r1));
        assertNotNull(new Instruction.SetRef(r0, r1, r2));
        assertNotNull(new Instruction.DeleteRef(r0, r1));
        assertNotNull(new Instruction.RefSpecial(r0, r1, "name"));
    }

    @Test
    void allTerminatorVariantsCanBeConstructed() {
        Register r0 = new Register(0);
        BlockId b0 = new BlockId(0);
        BlockId b1 = new BlockId(1);

        assertNotNull(new Terminator.Jump(b0));
        assertNotNull(new Terminator.CondJump(r0, b0, b1));
        assertNotNull(new Terminator.CondJumpNullUndef(r0, b0, b1));
        assertNotNull(new Terminator.Return(r0));
        assertNotNull(new Terminator.ReturnVoid());
        assertNotNull(new Terminator.Throw(r0, 1));
        assertNotNull(new Terminator.Rethrow(r0));
        assertNotNull(
                new Terminator.Switch(
                        r0, List.of(new Terminator.SwitchCase(new Register(1), b0)), b1));
        assertNotNull(new Terminator.GoSub(b0, b1));
    }
}
