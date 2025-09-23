package org.mozilla.javascript;

import static org.mozilla.javascript.Icode.Icode_INTNUMBER;
import static org.mozilla.javascript.Icode.Icode_POP_RESULT;
import static org.mozilla.javascript.Icode.Icode_REG_IND_C0;
import static org.mozilla.javascript.Icode.Icode_REG_STR_C0;
import static org.mozilla.javascript.Icode.Icode_UNDEF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.mozilla.javascript.ir.ConstantValue;
import org.mozilla.javascript.ir.ConstantValue.ConstantBoolean;
import org.mozilla.javascript.ir.ConstantValue.ConstantDouble;
import org.mozilla.javascript.ir.ConstantValue.ConstantInt;
import org.mozilla.javascript.ir.ConstantValue.ConstantNull;
import org.mozilla.javascript.ir.ConstantValue.ConstantUndefined;
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
    private final ByteCodeBuilder builder = new ByteCodeBuilder();
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
        builder.addToken(Token.RETURN_RESULT);

        idata.itsStringTable = builder.buildStringTable();
        idata.itsDoubleTable = builder.buildDoubleTable();
        idata.argNames = new String[0];
        idata.itsICode = builder.build();
        idata.itsMaxStack = maxStackDepth;
        idata.itsMaxFrameArray = maxStackDepth;

        return idata;
    }

    private void generateCode(IRScript ir) {
        for (IRInstruction instruction : ir.instructions()) {
            if (instruction instanceof PushConstant push) {
                generatePushConstant(push);
            } else if (instruction instanceof PopResult) {
                builder.addIcode(Icode_POP_RESULT);
                changeStack(-1);
            } else if (instruction instanceof Add) {
                builder.addToken(Token.ADD);
                changeStack(-1);
            } else if (instruction instanceof Sub) {
                builder.addToken(Token.SUB);
                changeStack(-1);
            } else if (instruction instanceof Mul) {
                builder.addToken(Token.MUL);
                changeStack(-1);
            } else if (instruction instanceof Div) {
                builder.addToken(Token.DIV);
                changeStack(-1);
            } else if (instruction instanceof Neg) {
                builder.addToken(Token.NEG);
                // No change in stack
            } else if (instruction instanceof Typeof) {
                builder.addToken(Token.TYPEOF);
                // No change in stack
            } else if (instruction instanceof Not) {
                builder.addToken(Token.NOT);
                // No change in stack
            } else {
                throw new UnsupportedOperationException("TODO: " + instruction);
            }
        }
    }

    private void generatePushConstant(PushConstant push) {
        if (push.value() instanceof ConstantInt cInt) {
            // TODO: shortnum etc
            builder.addIcode(Icode_INTNUMBER);
            builder.addInt(cInt.value());
        } else if (push.value() instanceof ConstantDouble cDbl) {
            int index = builder.addDoubleConstant(cDbl.value());
            builder.addSetIndexRegister(index);
            builder.addToken(Token.NUMBER);
        } else if (push.value() instanceof ConstantValue.ConstantString cStr) {
            int index = builder.addStringConstant(cStr.value());
            builder.addSetStringRegister(index);
            builder.addToken(Token.STRING);
        } else if (push.value() instanceof ConstantBoolean b) {
            builder.addToken(b.value() ? Token.TRUE : Token.FALSE);
        } else if (push.value() instanceof ConstantNull) {
            builder.addToken(Token.NULL);
        } else if (push.value() instanceof ConstantUndefined) {
            builder.addIcode(Icode_UNDEF);
        } else {
            throw new UnsupportedOperationException("TODO: " + push.value());
        }

        changeStack(+1);
    }

    private void changeStack(int delta) {
        if (currStackDepth + delta < 0) Kit.codeBug();
        currStackDepth += delta;
        if (currStackDepth > maxStackDepth) {
            maxStackDepth = currStackDepth;
        }
    }

    private static final class ByteCodeBuilder {
        private byte[] byteCode = new byte[128];
        private int byteCodeUsed = 0;
        private double[] doubleConstants = new double[16];
        private int doubleIndex = 0;
        private final List<String> stringConstants = new ArrayList<>(16);

        byte[] build() {
            if (byteCodeUsed != byteCode.length) {
                return Arrays.copyOf(byteCode, byteCodeUsed);
            } else {
                return byteCode;
            }
        }

        double[] buildDoubleTable() {
            if (doubleIndex != doubleConstants.length) {
                return Arrays.copyOf(doubleConstants, doubleIndex);
            } else {
                return doubleConstants;
            }
        }

        String[] buildStringTable() {
            return stringConstants.toArray(new String[0]);
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

            ensureByteCodeLength(+1);
            byteCode[byteCodeUsed] = (byte) value;
            ++byteCodeUsed;
        }

        public void addInt(int value) {
            ensureByteCodeLength(+4);
            byteCode[byteCodeUsed] = (byte) (value >> 24);
            byteCode[byteCodeUsed + 1] = (byte) (value >> 16);
            byteCode[byteCodeUsed + 2] = (byte) (value >> 8);
            byteCode[byteCodeUsed + 3] = (byte) value;
            byteCodeUsed += 4;
        }

        private void ensureByteCodeLength(int delta) {
            if (byteCodeUsed + delta >= byteCode.length) {
                byteCode = Arrays.copyOf(byteCode, byteCode.length * 2);
            }
        }

        public int addDoubleConstant(double value) {
            int index = doubleIndex;

            if (doubleConstants.length == index) {
                doubleConstants = Arrays.copyOf(doubleConstants, doubleConstants.length * 2);
            }

            doubleConstants[index] = value;
            ++doubleIndex;
            return index;
        }

        public int addStringConstant(String value) {
            int index = stringConstants.size();

            // TODO: deduplication
            stringConstants.add(value);
            return index;
        }

        public void addSetIndexRegister(int index) {
            switch (index) {
                case 0 -> addIcode(Icode_REG_IND_C0);
                default -> throw new UnsupportedOperationException("TODO");
            }
        }

        public void addSetStringRegister(int index) {
            switch (index) {
                case 0 -> addIcode(Icode_REG_STR_C0);
                default -> throw new UnsupportedOperationException("TODO");
            }
        }
    }
}
