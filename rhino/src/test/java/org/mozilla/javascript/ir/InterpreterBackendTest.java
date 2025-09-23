package org.mozilla.javascript.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.InterpreterBackend;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.ast.AstRoot;

class InterpreterBackendTest {
    @Nested
    class BasicExpressions {
        @Test
        void numericLiterals() {
            assertInterpreterResult(1, "1");
        }

        @Test
        void basicArithmetic() {
            assertInterpreterResult(-5, "-3 * 2 + 1");
        }

        @Test
        void typeof() {
            assertInterpreterResult("number", "typeof 42");
        }

        @Test
        void booleanExpressions() {
            assertInterpreterResult(false, "!true");
        }
    }

    private void assertInterpreterResult(double expected, String source) {
        Object result = interpret(source);
        Number resultNumber = assertInstanceOf(Number.class, result);
        assertEquals(expected, resultNumber.doubleValue(), 0.000001);
    }

    private void assertInterpreterResult(Object expected, String source) {
        Object result = interpret(source);
        assertEquals(expected, result);
    }

    private Object interpret(String source) {
        try (Context cx = Context.enter()) {
            CompilerEnvirons compilerEnv = new CompilerEnvirons();
            compilerEnv.initFromContext(cx);
            Parser parser = new Parser(compilerEnv);
            AstRoot root = parser.parse(source, "test", 1);

            IRScript generated = new IRGenerator().generate(root);

            Script fn = new InterpreterBackend().generateScript(compilerEnv, generated, null);

            Scriptable scope = cx.initStandardObjects(new TopLevel());

            return fn.exec(cx, scope, scope);
        }
    }
}
