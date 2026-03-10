/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.adapter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Jump;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.compiler.ir.BinOp;
import org.mozilla.javascript.compiler.ir.BlockId;
import org.mozilla.javascript.compiler.ir.CallKind;
import org.mozilla.javascript.compiler.ir.CfgBuilder;
import org.mozilla.javascript.compiler.ir.CfgFunction;
import org.mozilla.javascript.compiler.ir.EnumKind;
import org.mozilla.javascript.compiler.ir.ExceptionHandler;
import org.mozilla.javascript.compiler.ir.FunctionKind;
import org.mozilla.javascript.compiler.ir.IncDecOp;
import org.mozilla.javascript.compiler.ir.Instruction;
import org.mozilla.javascript.compiler.ir.IrConstant;
import org.mozilla.javascript.compiler.ir.LiteralPropertyKind;
import org.mozilla.javascript.compiler.ir.Register;
import org.mozilla.javascript.compiler.ir.Terminator;
import org.mozilla.javascript.compiler.ir.UnOp;

/**
 * Converts the old Node-based IR (after NodeTransformer) into the new CFG-based IR. This is
 * temporary adapter code to enable testing against Rhino's full test suite.
 */
public final class NodeToCfg {

    private CfgBuilder builder;
    private CompilerEnvirons compilerEnv;
    private ScriptNode scriptOrFn;
    private boolean isInFunction;
    private Map<Node, BlockId> targetBlockMap;
    private Map<Node, Register> localBlockRegisters;
    private Deque<TryContext> tryStack;
    private boolean currentBlockTerminated;
    private Register useStackRegister;
    private Register scriptResultRegister;
    private int lineNumber = -1;

    private static final class TryContext {
        final List<BlockId> protectedBlocks = new ArrayList<>();
    }

    private NodeToCfg() {}

    public static CfgFunction convert(ScriptNode tree, CompilerEnvirons env) {
        NodeToCfg converter = new NodeToCfg();
        converter.compilerEnv = env;
        return converter.convertFunction(tree);
    }

    private CfgFunction convertFunction(ScriptNode node) {
        scriptOrFn = node;
        isInFunction = node instanceof FunctionNode;

        String name;
        if (isInFunction) {
            name = ((FunctionNode) node).getName();
        } else {
            name = "";
        }

        builder = new CfgBuilder(name, node.getSourceName());
        builder.setParamCount(node.getParamCount());
        builder.setVariableNames(node.getParamAndVarNames());
        builder.setIsConst(node.getParamAndVarConst());
        builder.setBaseLineNumber(node.getBaseLineno());
        builder.setEndLineNumber(node.getEndLineno());

        if (isInFunction) {
            FunctionNode fn = (FunctionNode) node;
            builder.setStrict(fn.isInStrictMode());
            builder.setGenerator(fn.isGenerator());
            builder.setES6Generator(fn.isES6Generator());
            builder.setArrow(fn.getFunctionType() == FunctionNode.ARROW_FUNCTION);
            builder.setMethod(fn.isMethod());
            builder.setExpressionClosure(fn.isExpressionClosure());
            builder.setNeedsActivation(fn.requiresActivation());
            builder.setHasRestParameter(fn.hasRestParameter());
            builder.setFunctionKind(toFunctionKind(fn.getFunctionType()));
        }

        targetBlockMap = new IdentityHashMap<>();
        localBlockRegisters = new IdentityHashMap<>();
        tryStack = new ArrayDeque<>();
        currentBlockTerminated = false;
        lineNumber = -1;

        emitNestedFunctions();

        BlockId entryBlock = builder.newBlock();
        switchToBlock(entryBlock);

        if (isInFunction) {
            FunctionNode fn = (FunctionNode) node;
            if (fn.isGenerator()) {
                emitGeneratorPrologue(fn);
            }
            Node body = fn.getLastChild();
            scanForTargets(body);
            visitStatement(body);
        } else {
            scriptResultRegister = builder.newRegister();
            builder.emit(
                    new Instruction.LoadConstant(
                            scriptResultRegister, new IrConstant.UndefinedConst()));

            scanForTargets(node);
            visitStatement(node);

            if (!currentBlockTerminated) {
                terminateBlock(new Terminator.Return(scriptResultRegister));
            }
        }

        return builder.build();
    }

    private void emitNestedFunctions() {
        int functionCount = scriptOrFn.getFunctionCount();
        for (int i = 0; i < functionCount; i++) {
            FunctionNode fn = scriptOrFn.getFunctionNode(i);
            NodeToCfg nested = new NodeToCfg();
            nested.compilerEnv = compilerEnv;
            CfgFunction nestedFn = nested.convertFunction(fn);
            builder.addNestedFunction(nestedFn);
        }
    }

    private void emitGeneratorPrologue(FunctionNode fn) {
        Node paramInitBlock = fn.getGeneratorParamInitBlock();
        if (paramInitBlock != null) {
            scanForTargets(paramInitBlock);
            Node paramInit = paramInitBlock.getFirstChild();
            while (paramInit != null) {
                visitStatement(paramInit);
                paramInit = paramInit.getNext();
            }
        }

        int functionCount = fn.getFunctionCount();
        if (functionCount > 0) {
            for (int i = 0; i < functionCount; i++) {
                FunctionNode nested = fn.getFunctionNode(i);
                if (nested.getFunctionType() == FunctionNode.FUNCTION_STATEMENT) {
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.CreateClosure(dest, i, true, false));
                }
            }
        }

        builder.emit(new Instruction.GeneratorStart());
    }

    // --- Pass 1: scan for TARGET nodes ---

    private void scanForTargets(Node node) {
        if (node.getType() == Token.TARGET) {
            BlockId block = builder.newBlock();
            targetBlockMap.put(node, block);
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            scanForTargets(child);
        }
    }

    // --- Block management ---

    private void switchToBlock(BlockId block) {
        builder.setCurrentBlock(block);
        currentBlockTerminated = false;
        if (!tryStack.isEmpty()) {
            tryStack.peek().protectedBlocks.add(block);
        }
    }

    private void terminateBlock(Terminator terminator) {
        if (!currentBlockTerminated) {
            builder.terminate(terminator);
            currentBlockTerminated = true;
        }
    }

    private void emitLineNumber(Node node) {
        int lineno = node.getLineno();
        if (lineno != lineNumber && lineno >= 0) {
            lineNumber = lineno;
            builder.emit(new Instruction.Line(lineno));
        }
    }

    private Register getLocalBlockRegister(Node node) {
        // Follow the LOCAL_BLOCK_PROP indirection: most nodes (CATCH_SCOPE, RETHROW,
        // ENUM_INIT, ENUM_NEXT, ENUM_ID, LOCAL_LOAD) reference a LOCAL_BLOCK via this prop.
        Node localBlock = (Node) node.getProp(Node.LOCAL_BLOCK_PROP);
        if (localBlock != null) {
            node = localBlock;
        }
        Register reg = localBlockRegisters.get(node);
        if (reg == null) {
            reg = builder.newRegister();
            localBlockRegisters.put(node, reg);
        }
        return reg;
    }

    // --- Pass 2: visit statements ---

    private void visitStatement(Node node) {
        if (currentBlockTerminated) {
            // Dead code after a terminator - skip unless it's a TARGET
            if (node.getType() == Token.TARGET) {
                BlockId targetBlock = targetBlockMap.get(node);
                switchToBlock(targetBlock);
            }
            return;
        }

        int type = node.getType();
        Node child = node.getFirstChild();

        switch (type) {
            case Token.SCRIPT:
            case Token.BLOCK:
            case Token.EMPTY:
            case Token.WITH:
                {
                    if (type != Token.SCRIPT) {
                        emitLineNumber(node);
                    }
                    while (child != null) {
                        visitStatement(child);
                        child = child.getNext();
                    }
                    break;
                }

            case Token.LABEL:
            case Token.LOOP:
                {
                    emitLineNumber(node);
                    while (child != null) {
                        visitStatement(child);
                        child = child.getNext();
                    }
                    break;
                }

            case Token.TARGET:
                {
                    BlockId targetBlock = targetBlockMap.get(node);
                    if (!currentBlockTerminated) {
                        terminateBlock(new Terminator.Jump(targetBlock));
                    }
                    switchToBlock(targetBlock);
                    break;
                }

            case Token.GOTO:
                {
                    Node target = ((Jump) node).target;
                    BlockId targetBlock = targetBlockMap.get(target);
                    terminateBlock(new Terminator.Jump(targetBlock));
                    break;
                }

            case Token.IFEQ:
                {
                    Node target = ((Jump) node).target;
                    BlockId targetBlock = targetBlockMap.get(target);
                    BlockId fallthroughBlock = builder.newBlock();
                    Register cond = visitExpression(child);
                    terminateBlock(new Terminator.CondJump(cond, targetBlock, fallthroughBlock));
                    switchToBlock(fallthroughBlock);
                    break;
                }

            case Token.IFNE:
                {
                    Node target = ((Jump) node).target;
                    BlockId targetBlock = targetBlockMap.get(target);
                    BlockId fallthroughBlock = builder.newBlock();
                    Register cond = visitExpression(child);
                    terminateBlock(new Terminator.CondJump(cond, fallthroughBlock, targetBlock));
                    switchToBlock(fallthroughBlock);
                    break;
                }

            case Token.EXPR_VOID:
                {
                    emitLineNumber(node);
                    visitExpression(child);
                    break;
                }

            case Token.EXPR_RESULT:
                {
                    emitLineNumber(node);
                    Register result = visitExpression(child);
                    builder.emit(new Instruction.Move(scriptResultRegister, result));
                    break;
                }

            case Token.RETURN:
                {
                    emitLineNumber(node);
                    if (node.getIntProp(Node.GENERATOR_END_PROP, 0) != 0) {
                        if (child == null
                                || compilerEnv.getLanguageVersion() < Context.VERSION_ES6) {
                            builder.emit(new Instruction.GeneratorEnd());
                            terminateBlock(new Terminator.ReturnVoid());
                        } else {
                            Register value = visitExpression(child);
                            builder.emit(new Instruction.GeneratorReturn(value));
                            terminateBlock(new Terminator.ReturnVoid());
                        }
                    } else {
                        if (child == null) {
                            terminateBlock(new Terminator.ReturnVoid());
                        } else {
                            Register value = visitExpression(child);
                            terminateBlock(new Terminator.Return(value));
                        }
                    }
                    break;
                }

            case Token.RETURN_RESULT:
                {
                    emitLineNumber(node);
                    terminateBlock(new Terminator.Return(scriptResultRegister));
                    break;
                }

            case Token.THROW:
                {
                    emitLineNumber(node);
                    Register value = visitExpression(child);
                    terminateBlock(new Terminator.Throw(value, lineNumber));
                    break;
                }

            case Token.RETHROW:
                {
                    emitLineNumber(node);
                    Register exObj = getLocalBlockRegister(node);
                    terminateBlock(new Terminator.Rethrow(exObj));
                    break;
                }

            case Token.ENTERWITH:
                {
                    Register obj = visitExpression(child);
                    builder.emit(new Instruction.EnterWith(obj));
                    break;
                }

            case Token.LEAVEWITH:
                {
                    builder.emit(new Instruction.LeaveWith());
                    break;
                }

            case Token.DEBUGGER:
                {
                    builder.emit(new Instruction.Debugger());
                    break;
                }

            case Token.LOCAL_BLOCK:
                {
                    Register reg = builder.newRegister();
                    localBlockRegisters.put(node, reg);
                    builder.emit(
                            new Instruction.LoadConstant(reg, new IrConstant.UndefinedConst()));
                    emitLineNumber(node);
                    while (child != null) {
                        visitStatement(child);
                        child = child.getNext();
                    }
                    break;
                }

            case Token.FUNCTION:
                {
                    int fnIndex = node.getExistingIntProp(Node.FUNCTION_PROP);
                    int fnType = scriptOrFn.getFunctionNode(fnIndex).getFunctionType();
                    if (fnType == FunctionNode.FUNCTION_EXPRESSION_STATEMENT) {
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.CreateClosure(dest, fnIndex, true, false));
                    }

                    if (compilerEnv.getLanguageVersion() < Context.VERSION_ES6) {
                        if (!isInFunction) {
                            Register dest = builder.newRegister();
                            builder.emit(
                                    new Instruction.CreateClosure(dest, fnIndex, false, false));
                            builder.emit(new Instruction.Move(scriptResultRegister, dest));
                        }
                    }
                    break;
                }

            case Token.SWITCH:
                {
                    emitLineNumber(node);
                    Register discriminant = visitExpression(child);

                    List<Terminator.SwitchCase> cases = new ArrayList<>();
                    BlockId defaultTarget = null;

                    for (Jump caseNode = (Jump) child.getNext();
                            caseNode != null;
                            caseNode = (Jump) caseNode.getNext()) {
                        if (caseNode.getType() != Token.CASE) {
                            throw new RuntimeException("Expected CASE node: " + caseNode);
                        }
                        Node test = caseNode.getFirstChild();
                        Register caseValue = visitExpression(test);
                        BlockId caseTarget = targetBlockMap.get(caseNode.target);
                        cases.add(new Terminator.SwitchCase(caseValue, caseTarget));
                    }

                    // The default target is the next TARGET after the last CASE's target.
                    // In the old IR, after all CASE nodes come TARGET nodes for the cases
                    // and then the body. The default falls through to the first non-matched
                    // TARGET. We need to find a default; it's the goto target after the switch.
                    // Actually, looking at the Node structure: SWITCH has child = discriminant,
                    // then CASE nodes. The default is handled by a GOTO after all IFEQs in the
                    // transformed IR - but that's already been handled by NodeTransformer by
                    // the time we see it. Since the switch is decomposed into IFEQ/GOTO/TARGET
                    // chains by NodeTransformer, we should never actually see Token.SWITCH here.
                    // Let's handle it defensively anyway.

                    // If we got here, use a fallthrough block as default
                    BlockId fallthroughBlock = builder.newBlock();
                    defaultTarget = fallthroughBlock;

                    terminateBlock(new Terminator.Switch(discriminant, cases, defaultTarget));
                    switchToBlock(fallthroughBlock);
                    break;
                }

            case Token.TRY:
                {
                    Jump tryNode = (Jump) node;

                    // Get/create the register for the exception object. The LOCAL_BLOCK
                    // parent of the TRY allocates this register, and the catch handler
                    // needs the caught exception written into it.
                    Node tryLocalBlock = (Node) tryNode.getProp(Node.LOCAL_BLOCK_PROP);
                    Register exceptionReg = null;
                    if (tryLocalBlock != null) {
                        exceptionReg = localBlockRegisters.get(tryLocalBlock);
                    }

                    TryContext tryCtx = new TryContext();
                    tryStack.push(tryCtx);

                    while (child != null) {
                        visitStatement(child);
                        child = child.getNext();
                    }

                    tryStack.pop();

                    Node catchTarget = tryNode.target;
                    if (catchTarget != null) {
                        BlockId realCatchBlock = targetBlockMap.get(catchTarget);

                        // Create a landing pad block that loads the caught exception
                        BlockId landingPad = builder.newBlock();
                        BlockId savedBlock = builder.currentBlockId();
                        boolean savedTerminated = currentBlockTerminated;

                        switchToBlock(landingPad);
                        if (exceptionReg != null) {
                            builder.emit(new Instruction.GetCaughtException(exceptionReg));
                        }
                        terminateBlock(new Terminator.Jump(realCatchBlock));

                        // Restore previous block state
                        if (savedBlock != null) {
                            builder.setCurrentBlock(savedBlock);
                        }
                        currentBlockTerminated = savedTerminated;

                        builder.addExceptionHandler(
                                new ExceptionHandler(
                                        List.copyOf(tryCtx.protectedBlocks), landingPad, false));
                    }

                    Node finallyTarget = tryNode.getFinally();
                    if (finallyTarget != null) {
                        BlockId realFinallyBlock = targetBlockMap.get(finallyTarget);

                        // Create a landing pad for finally too
                        BlockId landingPad = builder.newBlock();
                        BlockId savedBlock = builder.currentBlockId();
                        boolean savedTerminated = currentBlockTerminated;

                        switchToBlock(landingPad);
                        if (exceptionReg != null) {
                            builder.emit(new Instruction.GetCaughtException(exceptionReg));
                        }
                        terminateBlock(new Terminator.Jump(realFinallyBlock));

                        if (savedBlock != null) {
                            builder.setCurrentBlock(savedBlock);
                        }
                        currentBlockTerminated = savedTerminated;

                        builder.addExceptionHandler(
                                new ExceptionHandler(
                                        List.copyOf(tryCtx.protectedBlocks), landingPad, true));
                    }
                    break;
                }

            case Token.CATCH_SCOPE:
                {
                    Register localReg = getLocalBlockRegister(node);
                    int scopeIndex = node.getExistingIntProp(Node.CATCH_SCOPE_PROP);
                    String name = child.getType() == Token.NAME ? child.getString() : "";
                    child = child.getNext();
                    Register exceptionObj = visitExpression(child);
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.EnterCatch(dest, name, scopeIndex, exceptionObj));
                    builder.emit(new Instruction.Move(localReg, dest));
                    break;
                }

            case Token.JSR:
                {
                    Node target = ((Jump) node).target;
                    BlockId targetBlock = targetBlockMap.get(target);
                    BlockId continuationBlock = builder.newBlock();
                    terminateBlock(new Terminator.GoSub(targetBlock, continuationBlock));
                    switchToBlock(continuationBlock);
                    break;
                }

            case Token.FINALLY:
                {
                    // In CFG form, STARTSUB/RETSUB are no-ops. The block splitting
                    // at TARGET nodes provides natural termination.
                    while (child != null) {
                        visitStatement(child);
                        child = child.getNext();
                    }
                    break;
                }

            case Token.ENUM_INIT_KEYS:
            case Token.ENUM_INIT_VALUES:
            case Token.ENUM_INIT_ARRAY:
            case Token.ENUM_INIT_VALUES_IN_ORDER:
                {
                    Register obj = visitExpression(child);
                    Register localReg = getLocalBlockRegister(node);
                    EnumKind kind = toEnumKind(type);
                    builder.emit(new Instruction.EnumInit(localReg, obj, kind));
                    break;
                }

            default:
                throw new RuntimeException("Unexpected statement node: " + Token.name(type));
        }
    }

    // --- Visit expressions ---

    private Register visitExpression(Node node) {
        if (currentBlockTerminated) {
            // Unreachable, but we need to return something
            return builder.newRegister();
        }

        int type = node.getType();
        Node child = node.getFirstChild();

        switch (type) {
            case Token.NUMBER:
                {
                    Register dest = builder.newRegister();
                    builder.emit(
                            new Instruction.LoadConstant(
                                    dest, new IrConstant.NumberConst(node.getDouble())));
                    return dest;
                }

            case Token.STRING:
                {
                    Register dest = builder.newRegister();
                    builder.emit(
                            new Instruction.LoadConstant(
                                    dest, new IrConstant.StringConst(node.getString())));
                    return dest;
                }

            case Token.BIGINT:
                {
                    Register dest = builder.newRegister();
                    builder.emit(
                            new Instruction.LoadConstant(
                                    dest, new IrConstant.BigIntConst(node.getBigInt())));
                    return dest;
                }

            case Token.TRUE:
                {
                    Register dest = builder.newRegister();
                    builder.emit(
                            new Instruction.LoadConstant(dest, new IrConstant.BooleanConst(true)));
                    return dest;
                }

            case Token.FALSE:
                {
                    Register dest = builder.newRegister();
                    builder.emit(
                            new Instruction.LoadConstant(dest, new IrConstant.BooleanConst(false)));
                    return dest;
                }

            case Token.NULL:
                {
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.LoadConstant(dest, new IrConstant.NullConst()));
                    return dest;
                }

            case Token.UNDEFINED:
                {
                    Register dest = builder.newRegister();
                    builder.emit(
                            new Instruction.LoadConstant(dest, new IrConstant.UndefinedConst()));
                    return dest;
                }

            case Token.THIS:
                {
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.GetThis(dest));
                    return dest;
                }

            case Token.THISFN:
                {
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.GetThisFn(dest));
                    return dest;
                }

            case Token.SUPER:
                {
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.GetSuper(dest));
                    return dest;
                }

            case Token.REGEXP:
                {
                    int index = node.getExistingIntProp(Node.REGEXP_PROP);
                    String pattern = scriptOrFn.getRegexpString(index);
                    String flags = scriptOrFn.getRegexpFlags(index);
                    Register dest = builder.newRegister();
                    builder.emit(
                            new Instruction.LoadConstant(
                                    dest, new IrConstant.RegExpConst(pattern, flags)));
                    return dest;
                }

            case Token.GETVAR:
                {
                    int index = scriptOrFn.getIndexForNameNode(node);
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.GetVar(dest, index));
                    return dest;
                }

            case Token.SETVAR:
                {
                    int index = scriptOrFn.getIndexForNameNode(child);
                    child = child.getNext();
                    Register value = visitExpression(child);
                    builder.emit(new Instruction.SetVar(index, value));
                    return value;
                }

            case Token.SETCONSTVAR:
                {
                    int index = scriptOrFn.getIndexForNameNode(child);
                    child = child.getNext();
                    Register value = visitExpression(child);
                    builder.emit(new Instruction.SetConstVar(index, value));
                    return value;
                }

            case Token.NAME:
                {
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.GetName(dest, node.getString()));
                    return dest;
                }

            case Token.BINDNAME:
                {
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.BindName(dest, node.getString()));
                    return dest;
                }

            case Token.SETNAME:
                {
                    String name = child.getString();
                    Register scope = visitExpression(child);
                    child = child.getNext();
                    Register value = visitExpression(child);
                    builder.emit(new Instruction.SetName(name, scope, value));
                    return value;
                }

            case Token.STRICT_SETNAME:
                {
                    String name = child.getString();
                    Register scope = visitExpression(child);
                    child = child.getNext();
                    Register value = visitExpression(child);
                    builder.emit(new Instruction.StrictSetName(name, scope, value));
                    return value;
                }

            case Token.SETCONST:
                {
                    String name = child.getString();
                    Register scope = visitExpression(child);
                    child = child.getNext();
                    Register value = visitExpression(child);
                    builder.emit(new Instruction.SetConst(name, scope, value));
                    return value;
                }

            case Token.TYPEOFNAME:
                {
                    int index = -1;
                    if (isInFunction && !scriptOrFn.isInStrictMode()) {
                        // Check if it's a local variable
                        FunctionNode fn = (FunctionNode) scriptOrFn;
                        if (!fn.requiresActivation()) {
                            index = scriptOrFn.getIndexForNameNode(node);
                        }
                    }
                    if (index == -1) {
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.TypeOfName(dest, node.getString()));
                        return dest;
                    } else {
                        Register varReg = builder.newRegister();
                        builder.emit(new Instruction.GetVar(varReg, index));
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.UnaryOp(dest, UnOp.TYPEOF, varReg));
                        return dest;
                    }
                }

            case Token.GETPROP:
            case Token.GETPROPNOWARN:
                {
                    Register obj = visitExpression(child);
                    child = child.getNext();
                    String property = child.getString();

                    if (node.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1) {
                        Register dest = builder.newRegister();
                        if (type == Token.GETPROP) {
                            builder.emit(new Instruction.GetPropOptional(dest, obj, property));
                        } else {
                            builder.emit(new Instruction.GetPropOptional(dest, obj, property));
                        }
                        return dest;
                    } else if (node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.GetPropSuper(dest, obj, property));
                        return dest;
                    } else {
                        Register dest = builder.newRegister();
                        if (type == Token.GETPROP) {
                            builder.emit(new Instruction.GetProp(dest, obj, property));
                        } else {
                            builder.emit(new Instruction.GetPropNoWarn(dest, obj, property));
                        }
                        return dest;
                    }
                }

            case Token.GETELEM:
                {
                    Register obj = visitExpression(child);
                    child = child.getNext();

                    if (node.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1) {
                        Register index = visitExpression(child);
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.GetElemOptional(dest, obj, index));
                        return dest;
                    } else if (node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                        Register index = visitExpression(child);
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.GetElemSuper(dest, obj, index));
                        return dest;
                    } else {
                        Register index = visitExpression(child);
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.GetElem(dest, obj, index));
                        return dest;
                    }
                }

            case Token.SETPROP:
            case Token.SETPROP_OP:
                {
                    Register obj = visitExpression(child);
                    child = child.getNext();
                    String property = child.getString();
                    child = child.getNext();

                    if (type == Token.SETPROP_OP) {
                        Register currentVal = builder.newRegister();
                        builder.emit(new Instruction.GetProp(currentVal, obj, property));
                        useStackRegister = currentVal;
                    }

                    Register value = visitExpression(child);
                    Register dest = builder.newRegister();
                    if (node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                        builder.emit(new Instruction.SetPropSuper(dest, obj, property, value));
                    } else {
                        builder.emit(new Instruction.SetProp(dest, obj, property, value));
                    }
                    return dest;
                }

            case Token.SETELEM:
            case Token.SETELEM_OP:
                {
                    Register obj = visitExpression(child);
                    child = child.getNext();
                    Register index = visitExpression(child);
                    child = child.getNext();

                    if (type == Token.SETELEM_OP) {
                        Register currentVal = builder.newRegister();
                        builder.emit(new Instruction.GetElem(currentVal, obj, index));
                        useStackRegister = currentVal;
                    }

                    Register value = visitExpression(child);
                    Register dest = builder.newRegister();
                    if (node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                        builder.emit(new Instruction.SetElemSuper(dest, obj, index, value));
                    } else {
                        builder.emit(new Instruction.SetElem(dest, obj, index, value));
                    }
                    return dest;
                }

            case Token.SET_REF:
            case Token.SET_REF_OP:
                {
                    Register ref = visitExpression(child);
                    child = child.getNext();

                    if (type == Token.SET_REF_OP) {
                        Register currentVal = builder.newRegister();
                        builder.emit(new Instruction.GetRef(currentVal, ref));
                        useStackRegister = currentVal;
                    }

                    Register value = visitExpression(child);
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.SetRef(dest, ref, value));
                    return dest;
                }

            case Token.DELPROP:
                {
                    boolean isName = child.getType() == Token.BINDNAME;
                    Register obj = visitExpression(child);
                    child = child.getNext();

                    if (node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.DeletePropSuper(dest));
                        // consume the child expression
                        visitExpression(child);
                        return dest;
                    } else if (isName) {
                        // delete name - the second child is a STRING with the name
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.DeleteName(dest, child.getString()));
                        return dest;
                    } else {
                        Register elem = visitExpression(child);
                        Register dest = builder.newRegister();
                        // Check if the element is a string constant (property name)
                        if (child.getType() == Token.STRING) {
                            builder.emit(new Instruction.DeleteProp(dest, obj, child.getString()));
                        } else {
                            builder.emit(new Instruction.DeleteElem(dest, obj, elem));
                        }
                        return dest;
                    }
                }

            case Token.USE_STACK:
                {
                    return useStackRegister;
                }

            case Token.LOCAL_LOAD:
                {
                    Register localReg = getLocalBlockRegister(node);
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.Move(dest, localReg));
                    return dest;
                }

            // --- Binary Operators ---

            case Token.ADD:
            case Token.SUB:
            case Token.MUL:
            case Token.DIV:
            case Token.MOD:
            case Token.EXP:
            case Token.BITOR:
            case Token.BITXOR:
            case Token.BITAND:
            case Token.LSH:
            case Token.RSH:
            case Token.URSH:
            case Token.EQ:
            case Token.NE:
            case Token.SHEQ:
            case Token.SHNE:
            case Token.LT:
            case Token.LE:
            case Token.GT:
            case Token.GE:
            case Token.IN:
            case Token.INSTANCEOF:
                {
                    Register left = visitExpression(child);
                    Register right = visitExpression(child.getNext());
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.BinaryOp(dest, toBinOp(type), left, right));
                    return dest;
                }

            case Token.STRING_CONCAT:
                {
                    Register left = visitExpression(child);
                    Register right = visitExpression(child.getNext());
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.BinaryOp(dest, BinOp.ADD, left, right));
                    return dest;
                }

            // --- Unary Operators ---

            case Token.POS:
            case Token.NEG:
            case Token.NOT:
            case Token.BITNOT:
            case Token.TYPEOF:
                {
                    Register operand = visitExpression(child);
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.UnaryOp(dest, toUnOp(type), operand));
                    return dest;
                }

            case Token.VOID:
                {
                    visitExpression(child);
                    Register dest = builder.newRegister();
                    builder.emit(
                            new Instruction.LoadConstant(dest, new IrConstant.UndefinedConst()));
                    return dest;
                }

            // --- Inc/Dec ---

            case Token.INC:
            case Token.DEC:
                return visitIncDec(node, child);

            // --- Short-circuit operators ---

            case Token.AND:
                {
                    Register result = builder.newRegister();
                    Register left = visitExpression(child);
                    builder.emit(new Instruction.Move(result, left));

                    BlockId evalRightBlock = builder.newBlock();
                    BlockId mergeBlock = builder.newBlock();

                    terminateBlock(new Terminator.CondJump(left, evalRightBlock, mergeBlock));

                    switchToBlock(evalRightBlock);
                    Register right = visitExpression(child.getNext());
                    builder.emit(new Instruction.Move(result, right));
                    terminateBlock(new Terminator.Jump(mergeBlock));

                    switchToBlock(mergeBlock);
                    return result;
                }

            case Token.OR:
                {
                    Register result = builder.newRegister();
                    Register left = visitExpression(child);
                    builder.emit(new Instruction.Move(result, left));

                    BlockId evalRightBlock = builder.newBlock();
                    BlockId mergeBlock = builder.newBlock();

                    terminateBlock(new Terminator.CondJump(left, mergeBlock, evalRightBlock));

                    switchToBlock(evalRightBlock);
                    Register right = visitExpression(child.getNext());
                    builder.emit(new Instruction.Move(result, right));
                    terminateBlock(new Terminator.Jump(mergeBlock));

                    switchToBlock(mergeBlock);
                    return result;
                }

            case Token.HOOK:
                {
                    Node ifThen = child.getNext();
                    Node ifElse = ifThen.getNext();
                    Register cond = visitExpression(child);

                    Register result = builder.newRegister();
                    BlockId thenBlock = builder.newBlock();
                    BlockId elseBlock = builder.newBlock();
                    BlockId mergeBlock = builder.newBlock();

                    terminateBlock(new Terminator.CondJump(cond, thenBlock, elseBlock));

                    switchToBlock(thenBlock);
                    Register thenVal = visitExpression(ifThen);
                    builder.emit(new Instruction.Move(result, thenVal));
                    if (!currentBlockTerminated) {
                        terminateBlock(new Terminator.Jump(mergeBlock));
                    }

                    switchToBlock(elseBlock);
                    Register elseVal = visitExpression(ifElse);
                    builder.emit(new Instruction.Move(result, elseVal));
                    if (!currentBlockTerminated) {
                        terminateBlock(new Terminator.Jump(mergeBlock));
                    }

                    switchToBlock(mergeBlock);
                    return result;
                }

            case Token.NULLISH_COALESCING:
                {
                    Register result = builder.newRegister();
                    Register left = visitExpression(child);
                    builder.emit(new Instruction.Move(result, left));

                    BlockId evalRightBlock = builder.newBlock();
                    BlockId mergeBlock = builder.newBlock();

                    terminateBlock(
                            new Terminator.CondJumpNullUndef(left, evalRightBlock, mergeBlock));

                    switchToBlock(evalRightBlock);
                    Register right = visitExpression(child.getNext());
                    builder.emit(new Instruction.Move(result, right));
                    terminateBlock(new Terminator.Jump(mergeBlock));

                    switchToBlock(mergeBlock);
                    return result;
                }

            case Token.COMMA:
                {
                    Node lastChild = node.getLastChild();
                    while (child != lastChild) {
                        visitExpression(child);
                        child = child.getNext();
                    }
                    return visitExpression(lastChild);
                }

            // --- Functions and calls ---

            case Token.FUNCTION:
                {
                    int fnIndex = node.getExistingIntProp(Node.FUNCTION_PROP);
                    FunctionNode fn = scriptOrFn.getFunctionNode(fnIndex);
                    boolean isMethod = fn.isMethodDefinition();
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.CreateClosure(dest, fnIndex, false, isMethod));
                    return dest;
                }

            case Token.CALL:
            case Token.NEW:
            case Token.REF_CALL:
                {
                    boolean isOptionalChainingCall =
                            node.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1;

                    if (type == Token.NEW) {
                        Register constructor = visitExpression(child);

                        List<Register> args = new ArrayList<>();
                        child = child.getNext();
                        while (child != null) {
                            args.add(visitExpression(child));
                            child = child.getNext();
                        }

                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.New(dest, constructor, args));
                        return dest;
                    } else {
                        Register fnReg = builder.newRegister();
                        Register thisReg = builder.newRegister();

                        generateCallFunAndThis(child, isOptionalChainingCall, fnReg, thisReg);

                        if (isOptionalChainingCall) {
                            return visitOptionalCall(
                                    fnReg, thisReg, node, child, builder.newRegister());
                        }

                        List<Register> args = new ArrayList<>();
                        child = child.getNext();
                        while (child != null) {
                            args.add(visitExpression(child));
                            child = child.getNext();
                        }

                        Register dest = builder.newRegister();

                        int callType = node.getIntProp(Node.SPECIALCALL_PROP, Node.NON_SPECIALCALL);
                        CallKind callKind;
                        if (callType != Node.NON_SPECIALCALL && type != Token.REF_CALL) {
                            callKind = CallKind.EVAL;
                        } else if (node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                            callKind = CallKind.SUPER;
                        } else {
                            callKind = CallKind.NORMAL;
                        }

                        builder.emit(new Instruction.Call(dest, fnReg, thisReg, args, callKind));
                        return dest;
                    }
                }

            // --- Literals ---

            case Token.ARRAYLIT:
                return visitArrayLiteral(node, child);

            case Token.OBJECTLIT:
                return visitObjectLiteral(node, child);

            case Token.OBJECT_REST:
                {
                    Register source = visitExpression(child);
                    Object[] excludedKeys = (Object[]) node.getProp(Node.OBJECT_IDS_PROP);

                    List<String> staticKeys = new ArrayList<>();
                    List<Register> computedKeys = new ArrayList<>();
                    if (excludedKeys != null) {
                        for (Object key : excludedKeys) {
                            if (key instanceof Node) {
                                computedKeys.add(visitExpression((Node) key));
                            } else {
                                staticKeys.add(key.toString());
                            }
                        }
                    }

                    Register dest = builder.newRegister();
                    builder.emit(
                            new Instruction.ObjectRest(dest, source, staticKeys, computedKeys));
                    return dest;
                }

            case Token.TEMPLATE_LITERAL:
                {
                    int index = node.getExistingIntProp(Node.TEMPLATE_LITERAL_PROP);
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.LoadTemplateLiteral(dest, index));
                    return dest;
                }

            // --- Yield ---

            case Token.YIELD:
                {
                    Register value;
                    if (child != null) {
                        value = visitExpression(child);
                    } else {
                        value = builder.newRegister();
                        builder.emit(
                                new Instruction.LoadConstant(
                                        value, new IrConstant.UndefinedConst()));
                    }
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.Yield(dest, value));
                    return dest;
                }

            case Token.YIELD_STAR:
                {
                    Register value;
                    if (child != null) {
                        value = visitExpression(child);
                    } else {
                        value = builder.newRegister();
                        builder.emit(
                                new Instruction.LoadConstant(
                                        value, new IrConstant.UndefinedConst()));
                    }
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.YieldStar(dest, value));
                    return dest;
                }

            // --- Scope ---

            case Token.WITHEXPR:
                {
                    Node enterWith = node.getFirstChild();
                    Node with = enterWith.getNext();
                    Register obj = visitExpression(enterWith.getFirstChild());
                    builder.emit(new Instruction.EnterWith(obj));
                    Register result = visitExpression(with.getFirstChild());
                    builder.emit(new Instruction.LeaveWith());
                    return result;
                }

            // --- Enumeration ---

            case Token.ENUM_NEXT:
                {
                    Register localReg = getLocalBlockRegister(node);
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.EnumNext(dest, localReg));
                    return dest;
                }

            case Token.ENUM_ID:
                {
                    Register localReg = getLocalBlockRegister(node);
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.EnumId(dest, localReg));
                    return dest;
                }

            // --- Refs ---

            case Token.GET_REF:
                {
                    Register ref = visitExpression(child);
                    if (node.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1) {
                        Register dest = builder.newRegister();
                        BlockId getRefBlock = builder.newBlock();
                        BlockId mergeBlock = builder.newBlock();

                        builder.emit(new Instruction.Move(dest, ref));
                        terminateBlock(
                                new Terminator.CondJumpNullUndef(ref, mergeBlock, getRefBlock));

                        switchToBlock(getRefBlock);
                        Register refResult = builder.newRegister();
                        builder.emit(new Instruction.GetRef(refResult, ref));
                        builder.emit(new Instruction.Move(dest, refResult));
                        terminateBlock(new Terminator.Jump(mergeBlock));

                        switchToBlock(mergeBlock);
                        return dest;
                    } else {
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.GetRef(dest, ref));
                        return dest;
                    }
                }

            case Token.DEL_REF:
                {
                    Register ref = visitExpression(child);
                    if (node.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1) {
                        Register dest = builder.newRegister();
                        BlockId delRefBlock = builder.newBlock();
                        BlockId mergeBlock = builder.newBlock();

                        builder.emit(new Instruction.Move(dest, ref));
                        terminateBlock(
                                new Terminator.CondJumpNullUndef(ref, mergeBlock, delRefBlock));

                        switchToBlock(delRefBlock);
                        Register delResult = builder.newRegister();
                        builder.emit(new Instruction.DeleteRef(delResult, ref));
                        builder.emit(new Instruction.Move(dest, delResult));
                        terminateBlock(new Terminator.Jump(mergeBlock));

                        switchToBlock(mergeBlock);
                        return dest;
                    } else {
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.DeleteRef(dest, ref));
                        return dest;
                    }
                }

            case Token.REF_SPECIAL:
                {
                    Register obj = visitExpression(child);
                    String name = (String) node.getProp(Node.NAME_PROP);

                    if (node.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1) {
                        Register dest = builder.newRegister();
                        BlockId refBlock = builder.newBlock();
                        BlockId mergeBlock = builder.newBlock();

                        Register undefReg = builder.newRegister();
                        builder.emit(
                                new Instruction.LoadConstant(
                                        undefReg, new IrConstant.UndefinedConst()));
                        builder.emit(new Instruction.Move(dest, undefReg));

                        terminateBlock(new Terminator.CondJumpNullUndef(obj, mergeBlock, refBlock));

                        switchToBlock(refBlock);
                        Register refResult = builder.newRegister();
                        builder.emit(new Instruction.RefSpecial(refResult, obj, name));
                        builder.emit(new Instruction.Move(dest, refResult));
                        terminateBlock(new Terminator.Jump(mergeBlock));

                        switchToBlock(mergeBlock);
                        return dest;
                    } else {
                        Register dest = builder.newRegister();
                        builder.emit(new Instruction.RefSpecial(dest, obj, name));
                        return dest;
                    }
                }

            // --- XML nodes (unsupported) ---

            case Token.DOTQUERY:
            case Token.REF_MEMBER:
            case Token.REF_NS_MEMBER:
            case Token.REF_NAME:
            case Token.REF_NS_NAME:
            case Token.DEFAULTNAMESPACE:
            case Token.ESCXMLATTR:
            case Token.ESCXMLTEXT:
                throw new UnsupportedOperationException("XML node type: " + Token.name(type));

            case Token.ARRAYCOMP:
                throw new UnsupportedOperationException("Array comprehensions not supported");

            default:
                throw new RuntimeException(
                        "Unexpected expression node: " + Token.name(type) + " (" + type + ")");
        }
    }

    // --- Call target resolution ---

    private void generateCallFunAndThis(
            Node left, boolean optional, Register destFn, Register destThis) {
        int type = left.getType();
        switch (type) {
            case Token.NAME:
                {
                    String name = left.getString();
                    builder.emit(new Instruction.NameAndThis(destFn, destThis, name, optional));
                    break;
                }
            case Token.GETPROP:
            case Token.GETELEM:
                {
                    Node target = left.getFirstChild();
                    Register obj = visitExpression(target);
                    Node id = target.getNext();
                    if (type == Token.GETPROP) {
                        String property = id.getString();
                        builder.emit(
                                new Instruction.PropAndThis(
                                        destFn, destThis, obj, property, optional));
                    } else {
                        Register index = visitExpression(id);
                        builder.emit(
                                new Instruction.ElemAndThis(
                                        destFn, destThis, obj, index, optional));
                    }
                    break;
                }
            default:
                {
                    Register value = visitExpression(left);
                    builder.emit(new Instruction.ValueAndThis(destFn, destThis, value, optional));
                    break;
                }
        }
    }

    private Register visitOptionalCall(
            Register fnReg, Register thisReg, Node node, Node firstChild, Register dest) {
        // For optional calls fn?.(), if fn is null/undefined, skip argument
        // evaluation and return undefined
        BlockId callBlock = builder.newBlock();
        BlockId undefinedBlock = builder.newBlock();
        BlockId mergeBlock = builder.newBlock();

        terminateBlock(new Terminator.CondJumpNullUndef(fnReg, undefinedBlock, callBlock));

        // The call block: evaluate args and call
        switchToBlock(callBlock);
        List<Register> args = new ArrayList<>();
        Node child = firstChild.getNext();
        while (child != null) {
            args.add(visitExpression(child));
            child = child.getNext();
        }

        int callType = node.getIntProp(Node.SPECIALCALL_PROP, Node.NON_SPECIALCALL);
        CallKind callKind;
        if (callType != Node.NON_SPECIALCALL) {
            callKind = CallKind.EVAL;
        } else if (node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
            callKind = CallKind.SUPER;
        } else {
            callKind = CallKind.NORMAL;
        }

        Register callResult = builder.newRegister();
        builder.emit(new Instruction.Call(callResult, fnReg, thisReg, args, callKind));
        builder.emit(new Instruction.Move(dest, callResult));
        terminateBlock(new Terminator.Jump(mergeBlock));

        // The undefined block: produce undefined
        switchToBlock(undefinedBlock);
        Register undefReg = builder.newRegister();
        builder.emit(new Instruction.LoadConstant(undefReg, new IrConstant.UndefinedConst()));
        builder.emit(new Instruction.Move(dest, undefReg));
        terminateBlock(new Terminator.Jump(mergeBlock));

        switchToBlock(mergeBlock);
        return dest;
    }

    // --- Inc/Dec ---

    private Register visitIncDec(Node node, Node child) {
        int incrDecrMask = node.getExistingIntProp(Node.INCRDECR_PROP);
        int childType = child.getType();

        if (child.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
            return visitSuperIncDec(node, child, childType, incrDecrMask);
        }

        IncDecOp op = toIncDecOp(incrDecrMask);

        switch (childType) {
            case Token.GETVAR:
                {
                    int index = scriptOrFn.getIndexForNameNode(child);
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.IncDecVar(dest, op, index));
                    return dest;
                }
            case Token.NAME:
                {
                    String name = child.getString();
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.IncDecName(dest, op, name));
                    return dest;
                }
            case Token.GETPROP:
                {
                    Node object = child.getFirstChild();
                    Register obj = visitExpression(object);
                    String property = object.getNext().getString();
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.IncDecProp(dest, op, obj, property));
                    return dest;
                }
            case Token.GETELEM:
                {
                    Node object = child.getFirstChild();
                    Register obj = visitExpression(object);
                    Register index = visitExpression(object.getNext());
                    Register dest = builder.newRegister();
                    builder.emit(new Instruction.IncDecElem(dest, op, obj, index));
                    return dest;
                }
            case Token.GET_REF:
                {
                    // For ref inc/dec, we expand it inline:
                    // ref, get_ref, inc/dec, set_ref
                    Node ref = child.getFirstChild();
                    Register refReg = visitExpression(ref);
                    Register oldVal = builder.newRegister();
                    builder.emit(new Instruction.GetRef(oldVal, refReg));

                    boolean isPost = (incrDecrMask & Node.POST_FLAG) != 0;
                    boolean isDecr = (incrDecrMask & Node.DECR_FLAG) != 0;

                    Register one = builder.newRegister();
                    builder.emit(
                            new Instruction.LoadConstant(one, new IrConstant.NumberConst(1.0)));

                    Register newVal = builder.newRegister();
                    builder.emit(
                            new Instruction.BinaryOp(
                                    newVal, isDecr ? BinOp.SUB : BinOp.ADD, oldVal, one));

                    Register setResult = builder.newRegister();
                    builder.emit(new Instruction.SetRef(setResult, refReg, newVal));

                    return isPost ? oldVal : newVal;
                }
            default:
                throw new RuntimeException("Unexpected inc/dec child: " + Token.name(childType));
        }
    }

    private Register visitSuperIncDec(Node node, Node child, int childType, int incrDecrMask) {
        Node object = child.getFirstChild();
        boolean isPost = (incrDecrMask & Node.POST_FLAG) != 0;
        boolean isDecr = (incrDecrMask & Node.DECR_FLAG) != 0;

        // Get the old value
        Register superReg = visitExpression(object);
        Register oldVal;

        switch (childType) {
            case Token.GETPROP:
                {
                    String property = object.getNext().getString();
                    oldVal = builder.newRegister();
                    builder.emit(new Instruction.GetPropSuper(oldVal, superReg, property));
                    break;
                }
            case Token.GETELEM:
                {
                    Register index = visitExpression(object.getNext());
                    oldVal = builder.newRegister();
                    builder.emit(new Instruction.GetElemSuper(oldVal, superReg, index));
                    break;
                }
            default:
                throw new RuntimeException("Unexpected super inc/dec child: " + childType);
        }

        // Compute new value
        Register one = builder.newRegister();
        builder.emit(new Instruction.LoadConstant(one, new IrConstant.NumberConst(1.0)));

        Register newVal = builder.newRegister();
        builder.emit(new Instruction.BinaryOp(newVal, isDecr ? BinOp.SUB : BinOp.ADD, oldVal, one));

        // Set the new value
        Register superReg2 = builder.newRegister();
        builder.emit(new Instruction.GetSuper(superReg2));

        switch (childType) {
            case Token.GETPROP:
                {
                    String property = object.getNext().getString();
                    Register setResult = builder.newRegister();
                    builder.emit(
                            new Instruction.SetPropSuper(setResult, superReg2, property, newVal));
                    break;
                }
            case Token.GETELEM:
                {
                    Register index = visitExpression(object.getNext());
                    Register setResult = builder.newRegister();
                    builder.emit(new Instruction.SetElemSuper(setResult, superReg2, index, newVal));
                    break;
                }
        }

        return isPost ? oldVal : newVal;
    }

    // --- Array/Object literals ---

    private Register visitArrayLiteral(Node node, Node child) {
        int count = 0;
        for (Node n = child; n != null; n = n.getNext()) {
            count++;
        }

        int[] skipIndexes = (int[]) node.getProp(Node.SKIP_INDEXES_PROP);
        int numberOfSpread = node.getIntProp(Node.NUMBER_OF_SPREAD, 0);
        int size = count - numberOfSpread;

        // Compute source positions for skip indexes
        int[] sourcePositions = null;
        if (skipIndexes != null) {
            sourcePositions = new int[count];
            int sourcePos = 0;
            int skipIdx = 0;
            for (int i = 0; i < count; i++) {
                while (skipIdx < skipIndexes.length && skipIndexes[skipIdx] == sourcePos) {
                    sourcePos++;
                    skipIdx++;
                }
                sourcePositions[i] = sourcePos;
                sourcePos++;
            }
        }

        Register array = builder.newRegister();
        builder.emit(new Instruction.NewArray(array, size));

        int childIdx = 0;
        while (child != null) {
            if (child.getType() == Token.DOTDOTDOT) {
                Register iterable = visitExpression(child.getFirstChild());
                builder.emit(new Instruction.ArraySpread(array, iterable));
            } else {
                Register value = visitLiteralValue(child);
                builder.emit(new Instruction.InitArrayElement(array, value));
            }
            child = child.getNext();
            childIdx++;
        }

        return array;
    }

    private Register visitObjectLiteral(Node node, Node child) {
        Object[] propertyIds = (Object[]) node.getProp(Node.OBJECT_IDS_PROP);

        Register obj = builder.newRegister();
        builder.emit(new Instruction.NewObject(obj));

        int i = 0;
        while (child != null) {
            Object propertyId = propertyIds == null ? null : propertyIds[i];
            LiteralPropertyKind kind = toLiteralPropertyKind(child);

            if (propertyId instanceof Node) {
                Node propNode = (Node) propertyId;
                if (propNode.getType() == Token.DOTDOTDOT) {
                    // Spread
                    Register spreadSource = visitExpression(propNode.getFirstChild());
                    builder.emit(new Instruction.ObjectSpread(obj, spreadSource));
                } else {
                    // Computed property
                    Register key = visitExpression(propNode.getFirstChild());
                    Register value = visitLiteralValue(child);
                    builder.emit(new Instruction.InitComputedProp(obj, key, value, kind));
                }
            } else if (propertyId instanceof String) {
                IrConstant key = new IrConstant.StringConst((String) propertyId);
                Register value = visitLiteralValue(child);
                builder.emit(new Instruction.InitProp(obj, key, value, kind));
            } else if (propertyId instanceof Integer) {
                IrConstant key = new IrConstant.NumberConst((Integer) propertyId);
                Register value = visitLiteralValue(child);
                builder.emit(new Instruction.InitProp(obj, key, value, kind));
            }

            child = child.getNext();
            i++;
        }

        return obj;
    }

    private Register visitLiteralValue(Node child) {
        int childType = child.getType();
        if (childType == Token.GET || childType == Token.SET || childType == Token.METHOD) {
            return visitExpression(child.getFirstChild());
        } else {
            return visitExpression(child);
        }
    }

    private LiteralPropertyKind toLiteralPropertyKind(Node child) {
        int childType = child.getType();
        switch (childType) {
            case Token.GET:
                return LiteralPropertyKind.GETTER;
            case Token.SET:
                return LiteralPropertyKind.SETTER;
            case Token.METHOD:
                return LiteralPropertyKind.METHOD;
            default:
                return LiteralPropertyKind.VALUE;
        }
    }

    // --- Converter helpers ---

    private static BinOp toBinOp(int tokenType) {
        switch (tokenType) {
            case Token.ADD:
                return BinOp.ADD;
            case Token.SUB:
                return BinOp.SUB;
            case Token.MUL:
                return BinOp.MUL;
            case Token.DIV:
                return BinOp.DIV;
            case Token.MOD:
                return BinOp.MOD;
            case Token.EXP:
                return BinOp.EXP;
            case Token.BITOR:
                return BinOp.BITOR;
            case Token.BITXOR:
                return BinOp.BITXOR;
            case Token.BITAND:
                return BinOp.BITAND;
            case Token.LSH:
                return BinOp.LSH;
            case Token.RSH:
                return BinOp.RSH;
            case Token.URSH:
                return BinOp.URSH;
            case Token.EQ:
                return BinOp.EQ;
            case Token.NE:
                return BinOp.NE;
            case Token.SHEQ:
                return BinOp.SHEQ;
            case Token.SHNE:
                return BinOp.SHNE;
            case Token.LT:
                return BinOp.LT;
            case Token.LE:
                return BinOp.LE;
            case Token.GT:
                return BinOp.GT;
            case Token.GE:
                return BinOp.GE;
            case Token.IN:
                return BinOp.IN;
            case Token.INSTANCEOF:
                return BinOp.INSTANCEOF;
            default:
                throw new RuntimeException("Unknown binary op token: " + tokenType);
        }
    }

    private static UnOp toUnOp(int tokenType) {
        switch (tokenType) {
            case Token.NEG:
                return UnOp.NEG;
            case Token.POS:
                return UnOp.POS;
            case Token.NOT:
                return UnOp.NOT;
            case Token.BITNOT:
                return UnOp.BITNOT;
            case Token.TYPEOF:
                return UnOp.TYPEOF;
            default:
                throw new RuntimeException("Unknown unary op token: " + tokenType);
        }
    }

    private static IncDecOp toIncDecOp(int incrDecrMask) {
        boolean isDecr = (incrDecrMask & Node.DECR_FLAG) != 0;
        boolean isPost = (incrDecrMask & Node.POST_FLAG) != 0;
        if (isDecr) {
            return isPost ? IncDecOp.POST_DEC : IncDecOp.PRE_DEC;
        } else {
            return isPost ? IncDecOp.POST_INC : IncDecOp.PRE_INC;
        }
    }

    private static EnumKind toEnumKind(int tokenType) {
        switch (tokenType) {
            case Token.ENUM_INIT_KEYS:
                return EnumKind.KEYS;
            case Token.ENUM_INIT_VALUES:
                return EnumKind.VALUES;
            case Token.ENUM_INIT_ARRAY:
                return EnumKind.ARRAY;
            case Token.ENUM_INIT_VALUES_IN_ORDER:
                return EnumKind.VALUES_IN_ORDER;
            default:
                throw new RuntimeException("Unknown enum init token: " + tokenType);
        }
    }

    private static FunctionKind toFunctionKind(int fnType) {
        switch (fnType) {
            case FunctionNode.FUNCTION_STATEMENT:
                return FunctionKind.STATEMENT;
            case FunctionNode.FUNCTION_EXPRESSION:
                return FunctionKind.EXPRESSION;
            case FunctionNode.FUNCTION_EXPRESSION_STATEMENT:
                return FunctionKind.EXPRESSION_STATEMENT;
            case FunctionNode.ARROW_FUNCTION:
                return FunctionKind.ARROW;
            default:
                return FunctionKind.STATEMENT;
        }
    }
}
