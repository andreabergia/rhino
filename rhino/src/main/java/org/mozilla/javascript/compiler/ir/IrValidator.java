/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Structural invariant checker for {@link CfgFunction}. */
public final class IrValidator {

    public enum Mode {
        NON_SSA,
        SSA
    }

    private IrValidator() {}

    public static List<String> validate(CfgFunction function, Mode mode) {
        List<String> errors = new ArrayList<>();
        validateCommon(function, errors);
        if (mode == Mode.SSA) {
            validateSsa(function, errors);
        }
        return List.copyOf(errors);
    }

    private static void validateCommon(CfgFunction fn, List<String> errors) {
        // Build block id set
        Set<BlockId> blockIds = new HashSet<>();
        Map<BlockId, BasicBlock> blockMap = new HashMap<>();
        for (BasicBlock block : fn.blocks()) {
            blockIds.add(block.id());
            blockMap.put(block.id(), block);
        }

        // 1. Entry block exists
        if (!blockIds.contains(fn.entryBlock())) {
            errors.add("Entry block B" + fn.entryBlock().id() + " not found in block list");
        }

        // 9. paramCount <= variableNames.length
        if (fn.paramCount() > fn.variableNames().length) {
            errors.add(
                    "paramCount ("
                            + fn.paramCount()
                            + ") > variableNames.length ("
                            + fn.variableNames().length
                            + ")");
        }

        // 10. isConst.length == variableNames.length
        if (fn.isConst().length != fn.variableNames().length) {
            errors.add(
                    "isConst.length ("
                            + fn.isConst().length
                            + ") != variableNames.length ("
                            + fn.variableNames().length
                            + ")");
        }

        Set<Integer> definedRegisters = new HashSet<>();

        for (BasicBlock block : fn.blocks()) {
            // 2. Every block has a non-null terminator
            if (block.terminator() == null) {
                errors.add("Block B" + block.id().id() + " has null terminator");
                continue;
            }

            // 3. All BlockId references in terminators point to existing blocks
            for (BlockId target : CfgAnalysis.terminatorTargets(block.terminator())) {
                if (!blockIds.contains(target)) {
                    errors.add(
                            "Block B"
                                    + block.id().id()
                                    + " terminator references non-existent block B"
                                    + target.id());
                }
            }

            // Check instructions
            for (Instruction instr : block.instructions()) {
                // 4. Register ids in range
                collectDefinedRegisters(instr, definedRegisters);
                checkRegisterRange(instr, fn.registerCount(), block.id(), errors);

                // 6. varIndex in range
                checkVarIndex(instr, fn.variableNames().length, block.id(), errors);

                // 7. functionIndex in range
                checkFunctionIndex(instr, fn.nestedFunctions().size(), block.id(), errors);
            }

            // Check terminator register ranges
            checkTerminatorRegisterRange(
                    block.terminator(), fn.registerCount(), block.id(), errors);
        }

        // 5. Every register used is defined somewhere
        checkUsedRegistersDefined(fn, definedRegisters, errors);

        // 8. Exception handler block references are valid
        for (int i = 0; i < fn.exceptionHandlers().size(); i++) {
            ExceptionHandler handler = fn.exceptionHandlers().get(i);
            if (!blockIds.contains(handler.handlerBlock())) {
                errors.add(
                        "Exception handler "
                                + i
                                + " references non-existent handler block B"
                                + handler.handlerBlock().id());
            }
            for (BlockId protectedBlock : handler.protectedBlocks()) {
                if (!blockIds.contains(protectedBlock)) {
                    errors.add(
                            "Exception handler "
                                    + i
                                    + " references non-existent protected block B"
                                    + protectedBlock.id());
                }
            }
        }
    }

    private static void validateSsa(CfgFunction fn, List<String> errors) {
        CfgAnalysis analysis = CfgAnalysis.compute(fn);

        // 11. Each register defined exactly once
        Map<Integer, String> registerDefs = new HashMap<>();
        for (BasicBlock block : fn.blocks()) {
            for (Instruction instr : block.instructions()) {
                for (int regId : getDefinedRegisterIds(instr)) {
                    String location = "B" + block.id().id();
                    String prev = registerDefs.put(regId, location);
                    if (prev != null) {
                        errors.add(
                                "Register r"
                                        + regId
                                        + " defined in both "
                                        + prev
                                        + " and "
                                        + location);
                    }
                }
            }
        }

        for (BasicBlock block : fn.blocks()) {
            boolean seenNonPhi = false;

            for (Instruction instr : block.instructions()) {
                if (instr instanceof Instruction.Phi phi) {
                    // 12. Phi after non-Phi
                    if (seenNonPhi) {
                        errors.add("Phi instruction after non-Phi in block B" + block.id().id());
                    }

                    // 13 & 14. Phi input count and predecessors
                    List<BlockId> preds = analysis.getPredecessors(block.id());
                    if (phi.inputs().size() != preds.size()) {
                        errors.add(
                                "Phi in B"
                                        + block.id().id()
                                        + " has "
                                        + phi.inputs().size()
                                        + " inputs but block has "
                                        + preds.size()
                                        + " predecessors");
                    }

                    Set<BlockId> phiPreds = new HashSet<>();
                    for (Instruction.PhiInput input : phi.inputs()) {
                        phiPreds.add(input.predecessor());
                    }
                    Set<BlockId> expectedPreds = new HashSet<>(preds);
                    if (!phiPreds.equals(expectedPreds)) {
                        errors.add(
                                "Phi in B" + block.id().id() + " has wrong predecessor BlockIds");
                    }
                } else {
                    seenNonPhi = true;
                }
            }

            // 15. Entry block has no Phi instructions
            if (block.id().equals(fn.entryBlock())) {
                for (Instruction instr : block.instructions()) {
                    if (instr instanceof Instruction.Phi) {
                        errors.add("Entry block B" + block.id().id() + " has Phi instructions");
                        break;
                    }
                }
            }
        }
    }

    private static void collectDefinedRegisters(Instruction instr, Set<Integer> defined) {
        for (int regId : getDefinedRegisterIds(instr)) {
            defined.add(regId);
        }
    }

    private static List<Integer> getDefinedRegisterIds(Instruction instr) {
        List<Integer> result = new ArrayList<>();
        if (instr instanceof Instruction.Phi phi) {
            result.add(phi.dest().id());
        } else if (instr instanceof Instruction.Move move) {
            result.add(move.dest().id());
        } else if (instr instanceof Instruction.LoadConstant lc) {
            result.add(lc.dest().id());
        } else if (instr instanceof Instruction.LoadTemplateLiteral lt) {
            result.add(lt.dest().id());
        } else if (instr instanceof Instruction.GetVar gv) {
            result.add(gv.dest().id());
        } else if (instr instanceof Instruction.GetName gn) {
            result.add(gn.dest().id());
        } else if (instr instanceof Instruction.BindName bn) {
            result.add(bn.dest().id());
        } else if (instr instanceof Instruction.TypeOfName tn) {
            result.add(tn.dest().id());
        } else if (instr instanceof Instruction.DeleteName dn) {
            result.add(dn.dest().id());
        } else if (instr instanceof Instruction.GetProp gp) {
            result.add(gp.dest().id());
        } else if (instr instanceof Instruction.GetPropNoWarn gpnw) {
            result.add(gpnw.dest().id());
        } else if (instr instanceof Instruction.GetPropSuper gps) {
            result.add(gps.dest().id());
        } else if (instr instanceof Instruction.SetProp sp) {
            result.add(sp.dest().id());
        } else if (instr instanceof Instruction.SetPropSuper sps) {
            result.add(sps.dest().id());
        } else if (instr instanceof Instruction.GetElem ge) {
            result.add(ge.dest().id());
        } else if (instr instanceof Instruction.GetElemSuper ges) {
            result.add(ges.dest().id());
        } else if (instr instanceof Instruction.SetElem se) {
            result.add(se.dest().id());
        } else if (instr instanceof Instruction.SetElemSuper ses) {
            result.add(ses.dest().id());
        } else if (instr instanceof Instruction.DeleteProp dp) {
            result.add(dp.dest().id());
        } else if (instr instanceof Instruction.DeletePropSuper dps) {
            result.add(dps.dest().id());
        } else if (instr instanceof Instruction.GetPropOptional gpo) {
            result.add(gpo.dest().id());
        } else if (instr instanceof Instruction.GetElemOptional geo) {
            result.add(geo.dest().id());
        } else if (instr instanceof Instruction.BinaryOp bop) {
            result.add(bop.dest().id());
        } else if (instr instanceof Instruction.UnaryOp uop) {
            result.add(uop.dest().id());
        } else if (instr instanceof Instruction.IncDecVar idv) {
            result.add(idv.dest().id());
        } else if (instr instanceof Instruction.IncDecName idn) {
            result.add(idn.dest().id());
        } else if (instr instanceof Instruction.IncDecProp idp) {
            result.add(idp.dest().id());
        } else if (instr instanceof Instruction.IncDecElem ide) {
            result.add(ide.dest().id());
        } else if (instr instanceof Instruction.NameAndThis nat) {
            result.add(nat.destFn().id());
            result.add(nat.destThis().id());
        } else if (instr instanceof Instruction.PropAndThis pat) {
            result.add(pat.destFn().id());
            result.add(pat.destThis().id());
        } else if (instr instanceof Instruction.ElemAndThis eat) {
            result.add(eat.destFn().id());
            result.add(eat.destThis().id());
        } else if (instr instanceof Instruction.ValueAndThis vat) {
            result.add(vat.destFn().id());
            result.add(vat.destThis().id());
        } else if (instr instanceof Instruction.Call call) {
            result.add(call.dest().id());
        } else if (instr instanceof Instruction.New n) {
            result.add(n.dest().id());
        } else if (instr instanceof Instruction.NewArray na) {
            result.add(na.dest().id());
        } else if (instr instanceof Instruction.NewObject no) {
            result.add(no.dest().id());
        } else if (instr instanceof Instruction.ObjectRest or) {
            result.add(or.dest().id());
        } else if (instr instanceof Instruction.CreateClosure cc) {
            result.add(cc.dest().id());
        } else if (instr instanceof Instruction.EnterCatch ec) {
            result.add(ec.dest().id());
        } else if (instr instanceof Instruction.EnumInit ei) {
            result.add(ei.dest().id());
        } else if (instr instanceof Instruction.EnumNext en) {
            result.add(en.dest().id());
        } else if (instr instanceof Instruction.EnumId eid) {
            result.add(eid.dest().id());
        } else if (instr instanceof Instruction.GetThis gt) {
            result.add(gt.dest().id());
        } else if (instr instanceof Instruction.GetThisFn gtf) {
            result.add(gtf.dest().id());
        } else if (instr instanceof Instruction.GetSuper gs) {
            result.add(gs.dest().id());
        } else if (instr instanceof Instruction.GetNewTarget gnt) {
            result.add(gnt.dest().id());
        } else if (instr instanceof Instruction.Yield y) {
            result.add(y.dest().id());
        } else if (instr instanceof Instruction.YieldStar ys) {
            result.add(ys.dest().id());
        } else if (instr instanceof Instruction.GetRef gref) {
            result.add(gref.dest().id());
        } else if (instr instanceof Instruction.SetRef sref) {
            result.add(sref.dest().id());
        } else if (instr instanceof Instruction.DeleteRef dref) {
            result.add(dref.dest().id());
        } else if (instr instanceof Instruction.RefSpecial rs) {
            result.add(rs.dest().id());
        }
        return result;
    }

    private static List<Integer> getUsedRegisterIds(Instruction instr) {
        List<Integer> result = new ArrayList<>();
        if (instr instanceof Instruction.Phi phi) {
            for (Instruction.PhiInput input : phi.inputs()) {
                result.add(input.value().id());
            }
        } else if (instr instanceof Instruction.Move move) {
            result.add(move.src().id());
        } else if (instr instanceof Instruction.SetVar sv) {
            result.add(sv.src().id());
        } else if (instr instanceof Instruction.SetConstVar scv) {
            result.add(scv.src().id());
        } else if (instr instanceof Instruction.SetName sn) {
            result.add(sn.scope().id());
            result.add(sn.value().id());
        } else if (instr instanceof Instruction.StrictSetName ssn) {
            result.add(ssn.scope().id());
            result.add(ssn.value().id());
        } else if (instr instanceof Instruction.SetConst sc) {
            result.add(sc.scope().id());
            result.add(sc.value().id());
        } else if (instr instanceof Instruction.GetProp gp) {
            result.add(gp.object().id());
        } else if (instr instanceof Instruction.GetPropNoWarn gpnw) {
            result.add(gpnw.object().id());
        } else if (instr instanceof Instruction.GetPropSuper gps) {
            result.add(gps.object().id());
        } else if (instr instanceof Instruction.SetProp sp) {
            result.add(sp.object().id());
            result.add(sp.value().id());
        } else if (instr instanceof Instruction.SetPropSuper sps) {
            result.add(sps.object().id());
            result.add(sps.value().id());
        } else if (instr instanceof Instruction.GetElem ge) {
            result.add(ge.object().id());
            result.add(ge.index().id());
        } else if (instr instanceof Instruction.GetElemSuper ges) {
            result.add(ges.object().id());
            result.add(ges.index().id());
        } else if (instr instanceof Instruction.SetElem se) {
            result.add(se.object().id());
            result.add(se.index().id());
            result.add(se.value().id());
        } else if (instr instanceof Instruction.SetElemSuper ses) {
            result.add(ses.object().id());
            result.add(ses.index().id());
            result.add(ses.value().id());
        } else if (instr instanceof Instruction.DeleteProp dp) {
            result.add(dp.object().id());
            result.add(dp.index().id());
        } else if (instr instanceof Instruction.GetPropOptional gpo) {
            result.add(gpo.object().id());
        } else if (instr instanceof Instruction.GetElemOptional geo) {
            result.add(geo.object().id());
            result.add(geo.index().id());
        } else if (instr instanceof Instruction.BinaryOp bop) {
            result.add(bop.left().id());
            result.add(bop.right().id());
        } else if (instr instanceof Instruction.UnaryOp uop) {
            result.add(uop.operand().id());
        } else if (instr instanceof Instruction.IncDecProp idp) {
            result.add(idp.object().id());
        } else if (instr instanceof Instruction.IncDecElem ide) {
            result.add(ide.object().id());
            result.add(ide.index().id());
        } else if (instr instanceof Instruction.PropAndThis pat) {
            result.add(pat.object().id());
        } else if (instr instanceof Instruction.ElemAndThis eat) {
            result.add(eat.object().id());
            result.add(eat.index().id());
        } else if (instr instanceof Instruction.ValueAndThis vat) {
            result.add(vat.value().id());
        } else if (instr instanceof Instruction.Call call) {
            result.add(call.function().id());
            result.add(call.thisObj().id());
            for (Register arg : call.args()) {
                result.add(arg.id());
            }
        } else if (instr instanceof Instruction.New n) {
            result.add(n.constructor().id());
            for (Register arg : n.args()) {
                result.add(arg.id());
            }
        } else if (instr instanceof Instruction.InitProp ip) {
            result.add(ip.object().id());
            result.add(ip.value().id());
        } else if (instr instanceof Instruction.InitComputedProp icp) {
            result.add(icp.object().id());
            result.add(icp.key().id());
            result.add(icp.value().id());
        } else if (instr instanceof Instruction.InitArrayElement iae) {
            result.add(iae.array().id());
            result.add(iae.value().id());
        } else if (instr instanceof Instruction.ArraySpread as) {
            result.add(as.array().id());
            result.add(as.iterable().id());
        } else if (instr instanceof Instruction.ObjectSpread os) {
            result.add(os.object().id());
            result.add(os.source().id());
        } else if (instr instanceof Instruction.ObjectRest or) {
            result.add(or.source().id());
            for (Register r : or.computedExcludedKeys()) {
                result.add(r.id());
            }
        } else if (instr instanceof Instruction.EnterWith ew) {
            result.add(ew.object().id());
        } else if (instr instanceof Instruction.EnterCatch ec) {
            result.add(ec.exceptionObject().id());
        } else if (instr instanceof Instruction.EnumInit ei) {
            result.add(ei.object().id());
        } else if (instr instanceof Instruction.EnumNext en) {
            result.add(en.enumState().id());
        } else if (instr instanceof Instruction.EnumId eid) {
            result.add(eid.enumState().id());
        } else if (instr instanceof Instruction.Yield y) {
            result.add(y.value().id());
        } else if (instr instanceof Instruction.YieldStar ys) {
            result.add(ys.iterable().id());
        } else if (instr instanceof Instruction.GeneratorReturn gr) {
            result.add(gr.value().id());
        } else if (instr instanceof Instruction.GetRef gref) {
            result.add(gref.ref().id());
        } else if (instr instanceof Instruction.SetRef sref) {
            result.add(sref.ref().id());
            result.add(sref.value().id());
        } else if (instr instanceof Instruction.DeleteRef dref) {
            result.add(dref.ref().id());
        } else if (instr instanceof Instruction.RefSpecial rs) {
            result.add(rs.object().id());
        }
        return result;
    }

    private static void checkRegisterRange(
            Instruction instr, int registerCount, BlockId blockId, List<String> errors) {
        for (int regId : getDefinedRegisterIds(instr)) {
            if (regId < 0 || regId >= registerCount) {
                errors.add(
                        "Register r"
                                + regId
                                + " out of range [0, "
                                + registerCount
                                + ") in B"
                                + blockId.id());
            }
        }
        for (int regId : getUsedRegisterIds(instr)) {
            if (regId < 0 || regId >= registerCount) {
                errors.add(
                        "Register r"
                                + regId
                                + " out of range [0, "
                                + registerCount
                                + ") in B"
                                + blockId.id());
            }
        }
    }

    private static void checkTerminatorRegisterRange(
            Terminator term, int registerCount, BlockId blockId, List<String> errors) {
        for (int regId : getTerminatorRegisterIds(term)) {
            if (regId < 0 || regId >= registerCount) {
                errors.add(
                        "Register r"
                                + regId
                                + " out of range [0, "
                                + registerCount
                                + ") in terminator of B"
                                + blockId.id());
            }
        }
    }

    private static List<Integer> getTerminatorRegisterIds(Terminator term) {
        if (term instanceof Terminator.CondJump cj) {
            return List.of(cj.condition().id());
        } else if (term instanceof Terminator.CondJumpNullUndef cj) {
            return List.of(cj.value().id());
        } else if (term instanceof Terminator.Return r) {
            return List.of(r.value().id());
        } else if (term instanceof Terminator.Throw t) {
            return List.of(t.value().id());
        } else if (term instanceof Terminator.Rethrow r) {
            return List.of(r.exceptionObject().id());
        } else if (term instanceof Terminator.Switch s) {
            List<Integer> ids = new ArrayList<>();
            ids.add(s.value().id());
            for (Terminator.SwitchCase sc : s.cases()) {
                ids.add(sc.caseValue().id());
            }
            return ids;
        }
        return List.of();
    }

    private static void checkVarIndex(
            Instruction instr, int varCount, BlockId blockId, List<String> errors) {
        int varIndex = -1;
        if (instr instanceof Instruction.GetVar gv) {
            varIndex = gv.varIndex();
        } else if (instr instanceof Instruction.SetVar sv) {
            varIndex = sv.varIndex();
        } else if (instr instanceof Instruction.SetConstVar scv) {
            varIndex = scv.varIndex();
        } else if (instr instanceof Instruction.IncDecVar idv) {
            varIndex = idv.varIndex();
        }
        if (varIndex >= 0 && varIndex >= varCount) {
            errors.add(
                    "varIndex "
                            + varIndex
                            + " out of range [0, "
                            + varCount
                            + ") in B"
                            + blockId.id());
        }
    }

    private static void checkFunctionIndex(
            Instruction instr, int functionCount, BlockId blockId, List<String> errors) {
        if (instr instanceof Instruction.CreateClosure cc) {
            if (cc.functionIndex() < 0 || cc.functionIndex() >= functionCount) {
                errors.add(
                        "functionIndex "
                                + cc.functionIndex()
                                + " out of range [0, "
                                + functionCount
                                + ") in B"
                                + blockId.id());
            }
        }
    }

    private static void checkUsedRegistersDefined(
            CfgFunction fn, Set<Integer> definedRegisters, List<String> errors) {
        for (BasicBlock block : fn.blocks()) {
            for (Instruction instr : block.instructions()) {
                for (int regId : getUsedRegisterIds(instr)) {
                    if (!definedRegisters.contains(regId)) {
                        errors.add(
                                "Register r"
                                        + regId
                                        + " used in B"
                                        + block.id().id()
                                        + " but never defined");
                    }
                }
            }
            // Check terminator used registers
            for (int regId : getTerminatorRegisterIds(block.terminator())) {
                if (!definedRegisters.contains(regId)) {
                    errors.add(
                            "Register r"
                                    + regId
                                    + " used in terminator of B"
                                    + block.id().id()
                                    + " but never defined");
                }
            }
        }
    }
}
