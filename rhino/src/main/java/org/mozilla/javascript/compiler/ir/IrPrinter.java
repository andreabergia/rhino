/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import java.util.List;
import java.util.stream.Collectors;

/** Human-readable textual dump of a {@link CfgFunction}. */
public final class IrPrinter {

    private IrPrinter() {}

    public static String print(CfgFunction fn) {
        StringBuilder sb = new StringBuilder();
        printFunction(fn, sb, "");
        return sb.toString();
    }

    private static void printFunction(CfgFunction fn, StringBuilder sb, String indent) {
        // Header
        sb.append(indent).append("function ");
        sb.append(fn.name() != null ? fn.name() : "<anonymous>");
        sb.append('(');
        for (int i = 0; i < fn.paramCount(); i++) {
            if (i > 0) sb.append(", ");
            if (i < fn.variableNames().length) {
                sb.append(fn.variableNames()[i]);
            }
        }
        sb.append(")  // ");
        sb.append(fn.sourceName() != null ? fn.sourceName() : "<unknown>");
        sb.append(':').append(fn.baseLineNumber()).append('-').append(fn.endLineNumber());
        sb.append('\n');

        // Metadata
        sb.append(indent).append("  strict=").append(fn.isStrict());
        sb.append(" generator=").append(fn.isGenerator());
        sb.append(" arrow=").append(fn.isArrow());
        sb.append(" ssa=").append(fn.isSsa());
        sb.append('\n');

        sb.append(indent).append("  params=").append(fn.paramCount());
        sb.append(" vars=[");
        for (int i = 0; i < fn.variableNames().length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(fn.variableNames()[i]);
        }
        sb.append("] registers=").append(fn.registerCount());
        sb.append('\n');

        sb.append(indent).append("  entry=B").append(fn.entryBlock().id());
        sb.append('\n');

        // Exception handlers
        for (ExceptionHandler handler : fn.exceptionHandlers()) {
            sb.append(indent).append("  handler ");
            sb.append(
                    handler.protectedBlocks().stream()
                            .map(b -> "B" + b.id())
                            .collect(Collectors.joining(", ", "[", "]")));
            sb.append(" -> B").append(handler.handlerBlock().id());
            if (handler.isFinally()) {
                sb.append(" (finally)");
            }
            sb.append('\n');
        }

        // Blocks
        for (BasicBlock block : fn.blocks()) {
            sb.append('\n');
            sb.append(indent).append("  B").append(block.id().id()).append(":\n");
            for (Instruction instr : block.instructions()) {
                sb.append(indent).append("    ");
                printInstruction(instr, fn.variableNames(), sb);
                sb.append('\n');
            }
            sb.append(indent).append("    ");
            printTerminator(block.terminator(), sb);
            sb.append('\n');
        }

        // Nested functions
        for (int i = 0; i < fn.nestedFunctions().size(); i++) {
            sb.append('\n');
            sb.append(indent).append("  nested[").append(i).append("]:\n");
            printFunction(fn.nestedFunctions().get(i), sb, indent + "    ");
        }
    }

    private static void printInstruction(Instruction instr, String[] varNames, StringBuilder sb) {
        if (instr instanceof Instruction.Phi phi) {
            sb.append('r').append(phi.dest().id()).append(" = phi(");
            for (int i = 0; i < phi.inputs().size(); i++) {
                if (i > 0) sb.append(", ");
                Instruction.PhiInput input = phi.inputs().get(i);
                sb.append("B").append(input.predecessor().id());
                sb.append(": r").append(input.value().id());
            }
            sb.append(')');
        } else if (instr instanceof Instruction.Move move) {
            sb.append('r').append(move.dest().id());
            sb.append(" = move r").append(move.src().id());
        } else if (instr instanceof Instruction.Line line) {
            sb.append("line ").append(line.lineNumber());
        } else if (instr instanceof Instruction.Debugger) {
            sb.append("debugger");
        } else if (instr instanceof Instruction.LoadConstant lc) {
            sb.append('r').append(lc.dest().id()).append(" = const ");
            printConstant(lc.value(), sb);
        } else if (instr instanceof Instruction.LoadTemplateLiteral lt) {
            sb.append('r').append(lt.dest().id());
            sb.append(" = template_literal ").append(lt.templateIndex());
        } else if (instr instanceof Instruction.GetVar gv) {
            sb.append('r').append(gv.dest().id());
            sb.append(" = getvar ").append(gv.varIndex());
            appendVarComment(gv.varIndex(), varNames, sb);
        } else if (instr instanceof Instruction.SetVar sv) {
            sb.append("setvar ").append(sv.varIndex());
            sb.append(", r").append(sv.src().id());
            appendVarComment(sv.varIndex(), varNames, sb);
        } else if (instr instanceof Instruction.SetConstVar scv) {
            sb.append("setconstvar ").append(scv.varIndex());
            sb.append(", r").append(scv.src().id());
            appendVarComment(scv.varIndex(), varNames, sb);
        } else if (instr instanceof Instruction.GetName gn) {
            sb.append('r').append(gn.dest().id());
            sb.append(" = getname \"").append(gn.name()).append('"');
        } else if (instr instanceof Instruction.SetName sn) {
            sb.append("setname \"").append(sn.name()).append("\" r");
            sb.append(sn.scope().id()).append(", r").append(sn.value().id());
        } else if (instr instanceof Instruction.StrictSetName ssn) {
            sb.append("strict_setname \"").append(ssn.name()).append("\" r");
            sb.append(ssn.scope().id()).append(", r").append(ssn.value().id());
        } else if (instr instanceof Instruction.SetConst sc) {
            sb.append("setconst \"").append(sc.name()).append("\" r");
            sb.append(sc.scope().id()).append(", r").append(sc.value().id());
        } else if (instr instanceof Instruction.BindName bn) {
            sb.append('r').append(bn.dest().id());
            sb.append(" = bindname \"").append(bn.name()).append('"');
        } else if (instr instanceof Instruction.TypeOfName tn) {
            sb.append('r').append(tn.dest().id());
            sb.append(" = typeof_name \"").append(tn.name()).append('"');
        } else if (instr instanceof Instruction.DeleteName dn) {
            sb.append('r').append(dn.dest().id());
            sb.append(" = delete_name \"").append(dn.name()).append('"');
        } else if (instr instanceof Instruction.GetProp gp) {
            sb.append('r').append(gp.dest().id());
            sb.append(" = getprop r").append(gp.object().id());
            sb.append(".\"").append(gp.name()).append('"');
        } else if (instr instanceof Instruction.GetPropNoWarn gpnw) {
            sb.append('r').append(gpnw.dest().id());
            sb.append(" = getprop_nowarn r").append(gpnw.object().id());
            sb.append(".\"").append(gpnw.name()).append('"');
        } else if (instr instanceof Instruction.GetPropSuper gps) {
            sb.append('r').append(gps.dest().id());
            sb.append(" = getprop_super r").append(gps.object().id());
            sb.append(".\"").append(gps.name()).append('"');
        } else if (instr instanceof Instruction.SetProp sp) {
            sb.append('r').append(sp.dest().id());
            sb.append(" = setprop r").append(sp.object().id());
            sb.append(".\"").append(sp.name()).append("\" r").append(sp.value().id());
        } else if (instr instanceof Instruction.SetPropSuper sps) {
            sb.append('r').append(sps.dest().id());
            sb.append(" = setprop_super r").append(sps.object().id());
            sb.append(".\"").append(sps.name()).append("\" r").append(sps.value().id());
        } else if (instr instanceof Instruction.GetElem ge) {
            sb.append('r').append(ge.dest().id());
            sb.append(" = getelem r").append(ge.object().id());
            sb.append("[r").append(ge.index().id()).append(']');
        } else if (instr instanceof Instruction.GetElemSuper ges) {
            sb.append('r').append(ges.dest().id());
            sb.append(" = getelem_super r").append(ges.object().id());
            sb.append("[r").append(ges.index().id()).append(']');
        } else if (instr instanceof Instruction.SetElem se) {
            sb.append('r').append(se.dest().id());
            sb.append(" = setelem r").append(se.object().id());
            sb.append("[r").append(se.index().id()).append("] r").append(se.value().id());
        } else if (instr instanceof Instruction.SetElemSuper ses) {
            sb.append('r').append(ses.dest().id());
            sb.append(" = setelem_super r").append(ses.object().id());
            sb.append("[r").append(ses.index().id()).append("] r").append(ses.value().id());
        } else if (instr instanceof Instruction.DeleteProp dp) {
            sb.append('r').append(dp.dest().id());
            sb.append(" = delete_prop r").append(dp.object().id());
            sb.append(".\"").append(dp.name()).append('"');
        } else if (instr instanceof Instruction.DeleteElem de) {
            sb.append('r').append(de.dest().id());
            sb.append(" = delete_elem r").append(de.object().id());
            sb.append("[r").append(de.index().id()).append(']');
        } else if (instr instanceof Instruction.DeletePropSuper dps) {
            sb.append('r').append(dps.dest().id());
            sb.append(" = delete_prop_super");
        } else if (instr instanceof Instruction.GetPropOptional gpo) {
            sb.append('r').append(gpo.dest().id());
            sb.append(" = getprop_optional r").append(gpo.object().id());
            sb.append(".\"").append(gpo.name()).append('"');
        } else if (instr instanceof Instruction.GetElemOptional geo) {
            sb.append('r').append(geo.dest().id());
            sb.append(" = getelem_optional r").append(geo.object().id());
            sb.append("[r").append(geo.index().id()).append(']');
        } else if (instr instanceof Instruction.BinaryOp bop) {
            sb.append('r').append(bop.dest().id());
            sb.append(" = binop ").append(bop.op());
            sb.append(" r").append(bop.left().id());
            sb.append(", r").append(bop.right().id());
        } else if (instr instanceof Instruction.UnaryOp uop) {
            sb.append('r').append(uop.dest().id());
            sb.append(" = unop ").append(uop.op());
            sb.append(" r").append(uop.operand().id());
        } else if (instr instanceof Instruction.IncDecVar idv) {
            sb.append('r').append(idv.dest().id());
            sb.append(" = incdec_var ").append(idv.op());
            sb.append(' ').append(idv.varIndex());
            appendVarComment(idv.varIndex(), varNames, sb);
        } else if (instr instanceof Instruction.IncDecName idn) {
            sb.append('r').append(idn.dest().id());
            sb.append(" = incdec_name ").append(idn.op());
            sb.append(" \"").append(idn.name()).append('"');
        } else if (instr instanceof Instruction.IncDecProp idp) {
            sb.append('r').append(idp.dest().id());
            sb.append(" = incdec_prop ").append(idp.op());
            sb.append(" r").append(idp.object().id());
            sb.append(".\"").append(idp.name()).append('"');
        } else if (instr instanceof Instruction.IncDecElem ide) {
            sb.append('r').append(ide.dest().id());
            sb.append(" = incdec_elem ").append(ide.op());
            sb.append(" r").append(ide.object().id());
            sb.append("[r").append(ide.index().id()).append(']');
        } else if (instr instanceof Instruction.NameAndThis nat) {
            sb.append('r').append(nat.destFn().id());
            sb.append(", r").append(nat.destThis().id());
            sb.append(" = name_and_this \"").append(nat.name()).append('"');
            if (nat.optional()) sb.append(" optional");
        } else if (instr instanceof Instruction.PropAndThis pat) {
            sb.append('r').append(pat.destFn().id());
            sb.append(", r").append(pat.destThis().id());
            sb.append(" = prop_and_this r").append(pat.object().id());
            sb.append(".\"").append(pat.name()).append('"');
            if (pat.optional()) sb.append(" optional");
        } else if (instr instanceof Instruction.ElemAndThis eat) {
            sb.append('r').append(eat.destFn().id());
            sb.append(", r").append(eat.destThis().id());
            sb.append(" = elem_and_this r").append(eat.object().id());
            sb.append("[r").append(eat.index().id()).append(']');
            if (eat.optional()) sb.append(" optional");
        } else if (instr instanceof Instruction.ValueAndThis vat) {
            sb.append('r').append(vat.destFn().id());
            sb.append(", r").append(vat.destThis().id());
            sb.append(" = value_and_this r").append(vat.value().id());
            if (vat.optional()) sb.append(" optional");
        } else if (instr instanceof Instruction.Call call) {
            sb.append('r').append(call.dest().id());
            sb.append(" = call");
            if (call.callKind() != CallKind.NORMAL) {
                sb.append('[').append(call.callKind()).append(']');
            }
            sb.append(" r").append(call.function().id());
            sb.append(", r").append(call.thisObj().id());
            sb.append(", (");
            appendRegList(call.args(), sb);
            sb.append(')');
        } else if (instr instanceof Instruction.New n) {
            sb.append('r').append(n.dest().id());
            sb.append(" = new r").append(n.constructor().id());
            sb.append(" (");
            appendRegList(n.args(), sb);
            sb.append(')');
        } else if (instr instanceof Instruction.NewArray na) {
            sb.append('r').append(na.dest().id());
            sb.append(" = new_array ").append(na.size());
        } else if (instr instanceof Instruction.NewObject no) {
            sb.append('r').append(no.dest().id());
            sb.append(" = new_object");
        } else if (instr instanceof Instruction.InitProp ip) {
            sb.append("init_prop r").append(ip.object().id());
            sb.append('[');
            printConstant(ip.key(), sb);
            sb.append("] r").append(ip.value().id());
            sb.append(' ').append(ip.kind());
        } else if (instr instanceof Instruction.InitComputedProp icp) {
            sb.append("init_computed_prop r").append(icp.object().id());
            sb.append("[r").append(icp.key().id()).append("] r").append(icp.value().id());
            sb.append(' ').append(icp.kind());
        } else if (instr instanceof Instruction.InitArrayElement iae) {
            sb.append("init_array_elem r").append(iae.array().id());
            sb.append(", r").append(iae.value().id());
        } else if (instr instanceof Instruction.ArraySpread as) {
            sb.append("array_spread r").append(as.array().id());
            sb.append(", r").append(as.iterable().id());
        } else if (instr instanceof Instruction.ObjectSpread os) {
            sb.append("object_spread r").append(os.object().id());
            sb.append(", r").append(os.source().id());
        } else if (instr instanceof Instruction.ObjectRest or) {
            sb.append('r').append(or.dest().id());
            sb.append(" = object_rest r").append(or.source().id());
            sb.append(" exclude=[");
            sb.append(String.join(", ", or.excludedKeys()));
            sb.append(']');
        } else if (instr instanceof Instruction.CreateClosure cc) {
            sb.append('r').append(cc.dest().id());
            sb.append(" = create_closure ").append(cc.functionIndex());
            if (cc.isStatement()) sb.append(" stmt");
            if (cc.isMethod()) sb.append(" method");
        } else if (instr instanceof Instruction.EnterWith ew) {
            sb.append("enter_with r").append(ew.object().id());
        } else if (instr instanceof Instruction.LeaveWith) {
            sb.append("leave_with");
        } else if (instr instanceof Instruction.EnterCatch ec) {
            sb.append('r').append(ec.dest().id());
            sb.append(" = enter_catch \"").append(ec.name()).append("\" scope=");
            sb.append(ec.scopeIndex()).append(" r").append(ec.exceptionObject().id());
        } else if (instr instanceof Instruction.EnumInit ei) {
            sb.append('r').append(ei.dest().id());
            sb.append(" = enum_init r").append(ei.object().id());
            sb.append(' ').append(ei.kind());
        } else if (instr instanceof Instruction.EnumNext en) {
            sb.append('r').append(en.dest().id());
            sb.append(" = enum_next r").append(en.enumState().id());
        } else if (instr instanceof Instruction.EnumId eid) {
            sb.append('r').append(eid.dest().id());
            sb.append(" = enum_id r").append(eid.enumState().id());
        } else if (instr instanceof Instruction.GetThis gt) {
            sb.append('r').append(gt.dest().id()).append(" = get_this");
        } else if (instr instanceof Instruction.GetThisFn gtf) {
            sb.append('r').append(gtf.dest().id()).append(" = get_this_fn");
        } else if (instr instanceof Instruction.GetSuper gs) {
            sb.append('r').append(gs.dest().id()).append(" = get_super");
        } else if (instr instanceof Instruction.GetNewTarget gnt) {
            sb.append('r').append(gnt.dest().id()).append(" = get_new_target");
        } else if (instr instanceof Instruction.Yield y) {
            sb.append('r').append(y.dest().id());
            sb.append(" = yield r").append(y.value().id());
        } else if (instr instanceof Instruction.YieldStar ys) {
            sb.append('r').append(ys.dest().id());
            sb.append(" = yield* r").append(ys.iterable().id());
        } else if (instr instanceof Instruction.GeneratorStart) {
            sb.append("generator_start");
        } else if (instr instanceof Instruction.GeneratorReturn gr) {
            sb.append("generator_return r").append(gr.value().id());
        } else if (instr instanceof Instruction.GeneratorEnd) {
            sb.append("generator_end");
        } else if (instr instanceof Instruction.GetRef gref) {
            sb.append('r').append(gref.dest().id());
            sb.append(" = get_ref r").append(gref.ref().id());
        } else if (instr instanceof Instruction.SetRef sref) {
            sb.append('r').append(sref.dest().id());
            sb.append(" = set_ref r").append(sref.ref().id());
            sb.append(", r").append(sref.value().id());
        } else if (instr instanceof Instruction.DeleteRef dref) {
            sb.append('r').append(dref.dest().id());
            sb.append(" = delete_ref r").append(dref.ref().id());
        } else if (instr instanceof Instruction.RefSpecial rs) {
            sb.append('r').append(rs.dest().id());
            sb.append(" = ref_special r").append(rs.object().id());
            sb.append(" \"").append(rs.name()).append('"');
        } else {
            sb.append("<unknown instruction: ").append(instr).append('>');
        }
    }

    private static void printTerminator(Terminator term, StringBuilder sb) {
        if (term instanceof Terminator.Jump j) {
            sb.append("jump B").append(j.target().id());
        } else if (term instanceof Terminator.CondJump cj) {
            sb.append("cond_jump r").append(cj.condition().id());
            sb.append(" ? B").append(cj.ifTrue().id());
            sb.append(" : B").append(cj.ifFalse().id());
        } else if (term instanceof Terminator.CondJumpNullUndef cj) {
            sb.append("cond_jump_null_undef r").append(cj.value().id());
            sb.append(" ? B").append(cj.ifNullUndef().id());
            sb.append(" : B").append(cj.ifNotNullUndef().id());
        } else if (term instanceof Terminator.Return r) {
            sb.append("return r").append(r.value().id());
        } else if (term instanceof Terminator.ReturnVoid) {
            sb.append("return_void");
        } else if (term instanceof Terminator.Throw t) {
            sb.append("throw r").append(t.value().id());
            sb.append(" line=").append(t.lineNumber());
        } else if (term instanceof Terminator.Rethrow r) {
            sb.append("rethrow r").append(r.exceptionObject().id());
        } else if (term instanceof Terminator.Switch s) {
            sb.append("switch r").append(s.value().id());
            for (Terminator.SwitchCase sc : s.cases()) {
                sb.append(" r").append(sc.caseValue().id());
                sb.append("->B").append(sc.target().id());
            }
            sb.append(" default->B").append(s.defaultTarget().id());
        } else if (term instanceof Terminator.GoSub gs) {
            sb.append("gosub B").append(gs.target().id());
            sb.append(" cont=B").append(gs.continuation().id());
        } else {
            sb.append("<unknown terminator: ").append(term).append('>');
        }
    }

    private static void printConstant(IrConstant constant, StringBuilder sb) {
        if (constant instanceof IrConstant.NumberConst n) {
            sb.append(n.value());
        } else if (constant instanceof IrConstant.StringConst s) {
            sb.append('"').append(s.value()).append('"');
        } else if (constant instanceof IrConstant.BigIntConst bi) {
            sb.append(bi.value()).append('n');
        } else if (constant instanceof IrConstant.BooleanConst b) {
            sb.append(b.value());
        } else if (constant instanceof IrConstant.NullConst) {
            sb.append("null");
        } else if (constant instanceof IrConstant.UndefinedConst) {
            sb.append("undefined");
        } else if (constant instanceof IrConstant.RegExpConst re) {
            sb.append('/').append(re.pattern()).append('/').append(re.flags());
        }
    }

    private static void appendVarComment(int varIndex, String[] varNames, StringBuilder sb) {
        if (varNames != null && varIndex >= 0 && varIndex < varNames.length) {
            // Pad to align comments
            sb.append("           // ").append(varNames[varIndex]);
        }
    }

    private static void appendRegList(List<Register> regs, StringBuilder sb) {
        for (int i = 0; i < regs.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append('r').append(regs.get(i).id());
        }
    }
}
