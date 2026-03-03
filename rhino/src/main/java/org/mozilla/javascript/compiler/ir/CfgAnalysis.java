/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Computes and caches derived CFG information from a {@link CfgFunction}. */
public final class CfgAnalysis {
    private final Map<BlockId, List<BlockId>> predecessors;
    private final Map<BlockId, List<BlockId>> successors;
    private final List<BlockId> reversePostOrder;

    private CfgAnalysis(
            Map<BlockId, List<BlockId>> predecessors,
            Map<BlockId, List<BlockId>> successors,
            List<BlockId> reversePostOrder) {
        this.predecessors = predecessors;
        this.successors = successors;
        this.reversePostOrder = reversePostOrder;
    }

    public static CfgAnalysis compute(CfgFunction function) {
        Map<BlockId, List<BlockId>> succs = new HashMap<>();
        Map<BlockId, List<BlockId>> preds = new HashMap<>();

        for (BasicBlock block : function.blocks()) {
            succs.put(block.id(), new ArrayList<>());
            preds.put(block.id(), new ArrayList<>());
        }

        for (BasicBlock block : function.blocks()) {
            List<BlockId> targets = terminatorTargets(block.terminator());
            succs.get(block.id()).addAll(targets);
            for (BlockId target : targets) {
                preds.get(target).add(block.id());
            }
        }

        // Freeze the maps
        Map<BlockId, List<BlockId>> frozenSuccs = new HashMap<>();
        Map<BlockId, List<BlockId>> frozenPreds = new HashMap<>();
        for (var entry : succs.entrySet()) {
            frozenSuccs.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        for (var entry : preds.entrySet()) {
            frozenPreds.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        List<BlockId> rpo = computeReversePostOrder(function.entryBlock(), frozenSuccs);

        return new CfgAnalysis(
                Collections.unmodifiableMap(frozenPreds),
                Collections.unmodifiableMap(frozenSuccs),
                List.copyOf(rpo));
    }

    public List<BlockId> getPredecessors(BlockId block) {
        List<BlockId> result = predecessors.get(block);
        return result != null ? result : List.of();
    }

    public List<BlockId> getSuccessors(BlockId block) {
        List<BlockId> result = successors.get(block);
        return result != null ? result : List.of();
    }

    public List<BlockId> getReversePostOrder() {
        return reversePostOrder;
    }

    static List<BlockId> terminatorTargets(Terminator terminator) {
        if (terminator instanceof Terminator.Jump j) {
            return List.of(j.target());
        } else if (terminator instanceof Terminator.CondJump cj) {
            return List.of(cj.ifTrue(), cj.ifFalse());
        } else if (terminator instanceof Terminator.CondJumpNullUndef cj) {
            return List.of(cj.ifNullUndef(), cj.ifNotNullUndef());
        } else if (terminator instanceof Terminator.Return r) {
            return List.of();
        } else if (terminator instanceof Terminator.ReturnVoid rv) {
            return List.of();
        } else if (terminator instanceof Terminator.Throw t) {
            return List.of();
        } else if (terminator instanceof Terminator.Rethrow r) {
            return List.of();
        } else if (terminator instanceof Terminator.Switch s) {
            List<BlockId> targets = new ArrayList<>();
            for (Terminator.SwitchCase sc : s.cases()) {
                targets.add(sc.target());
            }
            targets.add(s.defaultTarget());
            return List.copyOf(targets);
        } else if (terminator instanceof Terminator.GoSub gs) {
            return List.of(gs.target(), gs.continuation());
        } else {
            throw new IllegalArgumentException("Unknown terminator: " + terminator);
        }
    }

    private static List<BlockId> computeReversePostOrder(
            BlockId entry, Map<BlockId, List<BlockId>> successors) {
        List<BlockId> postOrder = new ArrayList<>();
        Set<BlockId> visited = new HashSet<>();
        Deque<StackFrame> stack = new ArrayDeque<>();

        stack.push(new StackFrame(entry, 0));
        visited.add(entry);

        while (!stack.isEmpty()) {
            StackFrame frame = stack.peek();
            List<BlockId> succs = successors.getOrDefault(frame.blockId, List.of());
            if (frame.childIndex < succs.size()) {
                BlockId child = succs.get(frame.childIndex);
                frame.childIndex++;
                if (visited.add(child)) {
                    stack.push(new StackFrame(child, 0));
                }
            } else {
                stack.pop();
                postOrder.add(frame.blockId);
            }
        }

        Collections.reverse(postOrder);
        return postOrder;
    }

    private static final class StackFrame {
        final BlockId blockId;
        int childIndex;

        StackFrame(BlockId blockId, int childIndex) {
            this.blockId = blockId;
            this.childIndex = childIndex;
        }
    }
}
