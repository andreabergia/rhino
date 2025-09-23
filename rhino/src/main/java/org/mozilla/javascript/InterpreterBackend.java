package org.mozilla.javascript;

import java.util.Arrays;
import org.mozilla.javascript.ir.ConstantValue;
import org.mozilla.javascript.ir.ConstantValue.ConstantInt;
import org.mozilla.javascript.ir.IRInstruction;
import org.mozilla.javascript.ir.IRInstruction.Add;
import org.mozilla.javascript.ir.IRInstruction.Div;
import org.mozilla.javascript.ir.IRInstruction.Mul;
import org.mozilla.javascript.ir.IRInstruction.Neg;
import org.mozilla.javascript.ir.IRInstruction.Not;
import org.mozilla.javascript.ir.IRInstruction.PopResult;
import org.mozilla.javascript.ir.IRInstruction.PushConstant;
import org.mozilla.javascript.ir.IRInstruction.Sub;
import org.mozilla.javascript.ir.IRInstruction.Typeof;
import org.mozilla.javascript.ir.IRScript;

public class InterpreterBackend {
    private final ByteCodeBuilder byteCode = new ByteCodeBuilder();
    private int currStackDepth;
    private int maxStackDepth;

    public Script generateScript(
            CompilerEnvirons compilerEnv, IRScript ir, Object staticSecurityDomain) {
        InterpreterData idata = generate(compilerEnv, ir);
        Interpreter.dumpICode(idata);
        return InterpretedFunction.createScript(idata, staticSecurityDomain);
    }

    private InterpreterData generate(CompilerEnvirons compilerEnv, IRScript ir) {
        InterpreterData idata =
                new InterpreterData(compilerEnv.getLanguageVersion(), "TODO", "TODO", false);

        idata.itsName = null; // TODO
        idata.itsSourceFile = "TODO";
        idata.itsNeedsActivation = false;
        idata.itsRequiresArgumentObject = false;
        idata.itsFunctionType = 0; // Script (TODO: make constant)
        idata.topLevel = true;
        idata.isES6Generator = false;
        idata.isShorthand = false;
        idata.parentData = null;
        idata.evalScriptFlag = false;
        idata.declaredAsFunctionExpression = false;

        // TODO: idata.itsStringTable
        // TODO: idata.itsDoubleTable
        // TODO: idata.itsBigIntTable
        // TODO: idata.itsNestedFunctions
        // TODO: idata.itsRegExpLiterals
        // TODO: idata.itsTemplateLiterals
        // TODO: idata.itsExceptionTable
        // TODO: idata.itsMaxVars
        // TODO: idata.itsMaxLocals
        // TODO: idata.argNames
        // TODO: idata.argIsConst
        // TODO: idata.argCount
        // TODO: idata.argsHasRest
        // TODO: idata.argsHasDefaults
        // TODO: idata.itsMaxCalleeArgs
        // TODO: idata.rawSource
        // TODO: idata.rawSourceStart
        // TODO: idata.rawSourceEnd
        // TODO: idata.literalIds
        // TODO: idata.longJumps
        // TODO: idata.firstLinePC

        generateCode(ir);
        byteCode.addToken(Token.RETURN_RESULT);

        idata.argNames = new String[0];
        idata.itsICode = byteCode.build();
        idata.itsMaxStack = maxStackDepth;
        idata.itsMaxFrameArray = maxStackDepth;

        return idata;
    }

    private void generateCode(IRScript ir) {
        for (IRInstruction instruction : ir.instructions()) {
            if (instruction instanceof PushConstant push) {
                generatePushConstant(push);
            } else if (instruction instanceof PopResult) {
                byteCode.addIcode(Icode.Icode_POP_RESULT);
                changeStack(-1);
            } else if (instruction instanceof Add) {
                byteCode.addToken(Token.ADD);
                changeStack(-1);
            } else if (instruction instanceof Sub) {
                byteCode.addToken(Token.SUB);
                changeStack(-1);
            } else if (instruction instanceof Mul) {
                byteCode.addToken(Token.MUL);
                changeStack(-1);
            } else if (instruction instanceof Div) {
                byteCode.addToken(Token.DIV);
                changeStack(-1);
            } else if (instruction instanceof Neg) {
                byteCode.addToken(Token.NEG);
                // No change in stack
            } else if (instruction instanceof Typeof) {
                byteCode.addToken(Token.TYPEOF);
                // No change in stack
            } else if (instruction instanceof Not) {
                byteCode.addToken(Token.NOT);
                // No change in stack
            } else {
                throw new UnsupportedOperationException("TODO: " + instruction);
            }
        }
    }

    private void generatePushConstant(PushConstant push) {
        if (push.value() instanceof ConstantInt cInt) {
            // TODO: shortnum etc
            byteCode.addIcode(Icode.Icode_INTNUMBER);
            byteCode.addInt(cInt.value());
            changeStack(+1);
        } else if (push.value() instanceof ConstantValue.ConstantBoolean b) {
            byteCode.addToken(b.value() ? Token.TRUE : Token.FALSE);
            changeStack(+1);
        } else {
            throw new UnsupportedOperationException("TODO: " + push.value());
        }
    }

    private void changeStack(int delta) {
        if (currStackDepth + delta < 0) Kit.codeBug();
        currStackDepth += delta;
        if (currStackDepth > maxStackDepth) {
            maxStackDepth = currStackDepth;
        }
    }

    private static final class ByteCodeBuilder {
        private byte[] array = new byte[128];
        private int used;

        byte[] build() {
            if (used != array.length) {
                return Arrays.copyOf(array, used);
            } else {
                return array;
            }
        }

        void addToken(int token) {
            if (!Icode.validTokenCode(token)) throw Kit.codeBug();
            addUint8(token);
        }

        void addIcode(int icode) {
            if (!Icode.validIcode(icode)) throw Kit.codeBug();
            // Write negative icode as uint8 bits
            addUint8(icode & 0xFF);
        }

        private void addUint8(int value) {
            if ((value & ~0xFF) != 0) throw Kit.codeBug();

            ensureLength(+1);
            array[used] = (byte) value;
            ++used;
        }

        public void addInt(int value) {
            ensureLength(+4);
            array[used] = (byte) (value >> 24);
            array[used + 1] = (byte) (value >> 16);
            array[used + 2] = (byte) (value >> 8);
            array[used + 3] = (byte) value;
            used += 4;
        }

        private void ensureLength(int delta) {
            if (used + delta >= array.length) {
                array = Arrays.copyOf(array, array.length * 2);
            }
        }
    }
}
