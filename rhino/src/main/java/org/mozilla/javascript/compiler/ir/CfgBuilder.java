/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Fluent builder for constructing {@link CfgFunction} instances. Produces non-SSA IR. */
public final class CfgBuilder {
    private final String name;
    private final String sourceName;

    private int nextRegisterId = 0;
    private int nextBlockId = 0;
    private BlockId currentBlock;

    private final Map<BlockId, List<Instruction>> blockInstructions = new HashMap<>();
    private final Map<BlockId, Terminator> blockTerminators = new HashMap<>();
    private final List<BlockId> blockOrder = new ArrayList<>();
    private final List<ExceptionHandler> exceptionHandlers = new ArrayList<>();
    private final List<CfgFunction> nestedFunctions = new ArrayList<>();

    private int paramCount;
    private String[] variableNames = new String[0];
    private boolean[] isConst = new boolean[0];
    private boolean isStrict;
    private boolean isGenerator;
    private boolean isES6Generator;
    private boolean isArrow;
    private boolean isMethod;
    private boolean isExpressionClosure;
    private boolean needsActivation;
    private boolean hasRestParameter;
    private FunctionKind functionKind = FunctionKind.STATEMENT;
    private int baseLineNumber;
    private int endLineNumber;

    public CfgBuilder(String name, String sourceName) {
        this.name = name;
        this.sourceName = sourceName;
    }

    public Register newRegister() {
        return new Register(nextRegisterId++);
    }

    public BlockId newBlock() {
        BlockId id = new BlockId(nextBlockId++);
        blockInstructions.put(id, new ArrayList<>());
        blockOrder.add(id);
        return id;
    }

    public void setCurrentBlock(BlockId block) {
        if (!blockInstructions.containsKey(block)) {
            throw new IllegalArgumentException("Unknown block: " + block);
        }
        this.currentBlock = block;
    }

    public BlockId currentBlockId() {
        return currentBlock;
    }

    public void emit(Instruction instruction) {
        if (currentBlock == null) {
            throw new IllegalStateException("No current block set");
        }
        blockInstructions.get(currentBlock).add(instruction);
    }

    public void terminate(Terminator terminator) {
        if (currentBlock == null) {
            throw new IllegalStateException("No current block set");
        }
        if (blockTerminators.containsKey(currentBlock)) {
            throw new IllegalStateException("Block " + currentBlock + " already has a terminator");
        }
        blockTerminators.put(currentBlock, terminator);
    }

    public void addExceptionHandler(ExceptionHandler handler) {
        exceptionHandlers.add(handler);
    }

    public int addNestedFunction(CfgFunction fn) {
        nestedFunctions.add(fn);
        return nestedFunctions.size() - 1;
    }

    public void setParamCount(int paramCount) {
        this.paramCount = paramCount;
    }

    public void setVariableNames(String[] variableNames) {
        this.variableNames = variableNames;
    }

    public void setIsConst(boolean[] isConst) {
        this.isConst = isConst;
    }

    public void setStrict(boolean isStrict) {
        this.isStrict = isStrict;
    }

    public void setGenerator(boolean isGenerator) {
        this.isGenerator = isGenerator;
    }

    public void setES6Generator(boolean isES6Generator) {
        this.isES6Generator = isES6Generator;
    }

    public void setArrow(boolean isArrow) {
        this.isArrow = isArrow;
    }

    public void setMethod(boolean isMethod) {
        this.isMethod = isMethod;
    }

    public void setExpressionClosure(boolean isExpressionClosure) {
        this.isExpressionClosure = isExpressionClosure;
    }

    public void setNeedsActivation(boolean needsActivation) {
        this.needsActivation = needsActivation;
    }

    public void setHasRestParameter(boolean hasRestParameter) {
        this.hasRestParameter = hasRestParameter;
    }

    public void setFunctionKind(FunctionKind functionKind) {
        this.functionKind = functionKind;
    }

    public void setBaseLineNumber(int baseLineNumber) {
        this.baseLineNumber = baseLineNumber;
    }

    public void setEndLineNumber(int endLineNumber) {
        this.endLineNumber = endLineNumber;
    }

    public CfgFunction build() {
        if (blockOrder.isEmpty()) {
            throw new IllegalStateException("No blocks defined");
        }

        List<BasicBlock> blocks = new ArrayList<>();
        for (BlockId id : blockOrder) {
            Terminator terminator = blockTerminators.get(id);
            if (terminator == null) {
                throw new IllegalStateException("Block " + id + " has no terminator");
            }
            blocks.add(new BasicBlock(id, List.copyOf(blockInstructions.get(id)), terminator));
        }

        return new CfgFunction(
                name,
                sourceName,
                baseLineNumber,
                endLineNumber,
                paramCount,
                variableNames.clone(),
                isConst.clone(),
                isStrict,
                isGenerator,
                isES6Generator,
                isArrow,
                isMethod,
                isExpressionClosure,
                needsActivation,
                hasRestParameter,
                functionKind,
                blockOrder.get(0),
                List.copyOf(blocks),
                List.copyOf(exceptionHandlers),
                List.copyOf(nestedFunctions),
                nextRegisterId,
                false);
    }
}
