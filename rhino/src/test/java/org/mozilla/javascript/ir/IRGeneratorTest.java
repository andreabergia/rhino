package org.mozilla.javascript.ir;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ir.ConstantValue.ConstantBoolean;
import org.mozilla.javascript.ir.ConstantValue.ConstantInt;
import org.mozilla.javascript.ir.IRInstruction.Add;
import org.mozilla.javascript.ir.IRInstruction.Mul;
import org.mozilla.javascript.ir.IRInstruction.Name;
import org.mozilla.javascript.ir.IRInstruction.Neg;
import org.mozilla.javascript.ir.IRInstruction.Not;
import org.mozilla.javascript.ir.IRInstruction.PopResult;
import org.mozilla.javascript.ir.IRInstruction.PushConstant;
import org.mozilla.javascript.ir.IRInstruction.Typeof;

class IRGeneratorTest {
    @Nested
    class ExpressionStatement {

        @Test
        void basicArithmetic() {
            assertIR(
                    "-3 * 2 + a",
                    List.of(
                            new PushConstant(new ConstantInt(3)),
                            new Neg(),
                            new PushConstant(new ConstantInt(2)),
                            new Mul(),
                            new Name("a"),
                            new Add(),
                            new PopResult()));
        }

        @Test
        void booleanExpressions() {
            assertIR(
                    "!true",
                    List.of(
                            new PushConstant(new ConstantBoolean(true)),
                            new Not(),
                            new PopResult()));
        }

        @Test
        void typeof() {
            assertIR(
                    "typeof 42",
                    List.of(new PushConstant(new ConstantInt(42)), new Typeof(), new PopResult()));
        }
    }

    private void assertIR(String source, List<IRInstruction> expected) {
        try (Context cx = Context.enter()) {
            CompilerEnvirons compilerEnv = new CompilerEnvirons();
            compilerEnv.initFromContext(cx);
            Parser parser = new Parser(compilerEnv);
            AstRoot root = parser.parse(source, "test", 1);

            IRScript generated = new IRGenerator().generate(root);

            assertEquals(expected, generated.instructions());
        }
    }
}
