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
import org.mozilla.javascript.ir.ConstantValue.ConstantNull;
import org.mozilla.javascript.ir.ConstantValue.ConstantString;
import org.mozilla.javascript.ir.ConstantValue.ConstantUndefined;
import org.mozilla.javascript.ir.IRInstruction.Binary;
import org.mozilla.javascript.ir.IRInstruction.BinaryOperator;
import org.mozilla.javascript.ir.IRInstruction.Name;
import org.mozilla.javascript.ir.IRInstruction.PopResult;
import org.mozilla.javascript.ir.IRInstruction.PushConstant;
import org.mozilla.javascript.ir.IRInstruction.Unary;
import org.mozilla.javascript.ir.IRInstruction.UnaryOperator;

class IRGeneratorTest {
    @Nested
    class ExpressionStatement {
        @Test
        void basicArithmetic() {
            assertIR(
                    "-3 * 2 + a",
                    List.of(
                            new PushConstant(new ConstantInt(3)),
                            new Unary(UnaryOperator.Neg),
                            new PushConstant(new ConstantInt(2)),
                            new Binary(BinaryOperator.Mul),
                            new Name("a"),
                            new Binary(BinaryOperator.Add),
                            new PopResult()));
        }

        @Test
        void booleanExpressions() {
            assertIR(
                    "!true",
                    List.of(
                            new PushConstant(new ConstantBoolean(true)),
                            new Unary(UnaryOperator.Not),
                            new PopResult()));
        }

        @Test
        void typeof() {
            assertIR(
                    "typeof 42",
                    List.of(
                            new PushConstant(new ConstantInt(42)),
                            new Unary(UnaryOperator.Typeof),
                            new PopResult()));
        }

        @Test
        void strings() {
            assertIR(
                    "'rhino'",
                    List.of(new PushConstant(new ConstantString("rhino")), new PopResult()));
        }

        @Test
        void nullLiteral() {
            assertIR("null", List.of(new PushConstant(ConstantNull.INSTANCE), new PopResult()));
        }

        @Test
        void undefinedLiteral() {
            assertIR(
                    "undefined",
                    List.of(new PushConstant(ConstantUndefined.INSTANCE), new PopResult()));
        }

        @Test
        void comparisonOperators() {
            assertIR(
                    "1 < 2",
                    List.of(
                            new PushConstant(new ConstantInt(1)),
                            new PushConstant(new ConstantInt(2)),
                            new Binary(BinaryOperator.Lt),
                            new PopResult()));
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
