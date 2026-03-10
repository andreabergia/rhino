/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IRFactory;
import org.mozilla.javascript.NodeTransformer;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.compiler.ir.CfgFunction;
import org.mozilla.javascript.compiler.ir.IrPrinter;
import org.mozilla.javascript.compiler.ir.IrValidator;

class NodeToCfgTest {

    private CfgFunction compile(String source) {
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        env.setRecordingComments(false);
        env.setRecordingLocalJsDocComments(false);
        env.setInterpretedMode(true);

        Parser parser = new Parser(env);
        AstRoot ast = parser.parse(source, "test.js", 1);

        IRFactory irf = new IRFactory(env, source);
        ScriptNode tree = irf.transformTree(ast);

        new NodeTransformer().transform(tree, env);

        return NodeToCfg.convert(tree, env);
    }

    private void assertValid(CfgFunction fn) {
        List<String> errors = IrValidator.validate(fn, IrValidator.Mode.NON_SSA);
        assertTrue(
                errors.isEmpty(),
                () ->
                        "Validation errors:\n"
                                + String.join("\n", errors)
                                + "\n\nIR:\n"
                                + IrPrinter.print(fn));
    }

    // --- Constants & simple expressions ---

    @Test
    void numberConstant() {
        CfgFunction fn = compile("42");
        assertValid(fn);
    }

    @Test
    void stringConstant() {
        CfgFunction fn = compile("'hello'");
        assertValid(fn);
    }

    @Test
    void booleanConstant() {
        CfgFunction fn = compile("true");
        assertValid(fn);
    }

    @Test
    void nullConstant() {
        CfgFunction fn = compile("null");
        assertValid(fn);
    }

    @Test
    void undefinedConstant() {
        CfgFunction fn = compile("undefined");
        assertValid(fn);
    }

    @Test
    void binaryExpression() {
        CfgFunction fn = compile("1 + 2");
        assertValid(fn);
    }

    @Test
    void unaryNot() {
        CfgFunction fn = compile("!true");
        assertValid(fn);
    }

    @Test
    void typeofName() {
        CfgFunction fn = compile("typeof x");
        assertValid(fn);
    }

    @Test
    void voidExpression() {
        CfgFunction fn = compile("void 0");
        assertValid(fn);
    }

    // --- Variables ---

    @Test
    void varDeclaration() {
        CfgFunction fn = compile("var x = 1");
        assertValid(fn);
    }

    @Test
    void varReadAndWrite() {
        CfgFunction fn = compile("var x = 1; x");
        assertValid(fn);
    }

    @Test
    void assignment() {
        CfgFunction fn = compile("x = 2");
        assertValid(fn);
    }

    // --- Control flow ---

    @Test
    void ifStatement() {
        CfgFunction fn = compile("if (x) y");
        assertValid(fn);
        assertTrue(fn.blocks().size() > 1, "Expected multiple blocks for if");
    }

    @Test
    void whileLoop() {
        CfgFunction fn = compile("while (x) y");
        assertValid(fn);
    }

    @Test
    void forBreak() {
        CfgFunction fn = compile("for (;;) break");
        assertValid(fn);
    }

    @Test
    void logicalAnd() {
        CfgFunction fn = compile("a && b");
        assertValid(fn);
        assertTrue(fn.blocks().size() >= 3, "AND should produce multiple blocks");
    }

    @Test
    void logicalOr() {
        CfgFunction fn = compile("a || b");
        assertValid(fn);
    }

    @Test
    void ternary() {
        CfgFunction fn = compile("a ? b : c");
        assertValid(fn);
    }

    @Test
    void nullishCoalescing() {
        CfgFunction fn = compile("a ?? b");
        assertValid(fn);
    }

    @Test
    void commaExpression() {
        CfgFunction fn = compile("1, 2, 3");
        assertValid(fn);
    }

    // --- Functions ---

    @Test
    void functionDeclaration() {
        CfgFunction fn = compile("function f() {}");
        assertValid(fn);
        assertEquals(1, fn.nestedFunctions().size());
    }

    @Test
    void functionCall() {
        CfgFunction fn = compile("f()");
        assertValid(fn);
    }

    @Test
    void newExpression() {
        CfgFunction fn = compile("new F()");
        assertValid(fn);
    }

    @Test
    void iife() {
        CfgFunction fn = compile("(function() {})()");
        assertValid(fn);
    }

    @Test
    void functionExpression() {
        CfgFunction fn = compile("var f = function() { return 1; }");
        assertValid(fn);
        assertEquals(1, fn.nestedFunctions().size());
        assertValid(fn.nestedFunctions().get(0));
    }

    @Test
    void arrowFunction() {
        CfgFunction fn = compile("var f = () => 1");
        assertValid(fn);
        assertEquals(1, fn.nestedFunctions().size());
        CfgFunction arrow = fn.nestedFunctions().get(0);
        assertValid(arrow);
        assertTrue(arrow.isArrow());
    }

    @Test
    void callWithArgs() {
        CfgFunction fn = compile("f(1, 2, 3)");
        assertValid(fn);
    }

    @Test
    void methodCall() {
        CfgFunction fn = compile("o.m()");
        assertValid(fn);
    }

    @Test
    void computedMethodCall() {
        CfgFunction fn = compile("o[k]()");
        assertValid(fn);
    }

    // --- Properties ---

    @Test
    void getProp() {
        CfgFunction fn = compile("o.x");
        assertValid(fn);
    }

    @Test
    void setProp() {
        CfgFunction fn = compile("o.x = 1");
        assertValid(fn);
    }

    @Test
    void getElem() {
        CfgFunction fn = compile("o[k]");
        assertValid(fn);
    }

    @Test
    void deleteProp() {
        CfgFunction fn = compile("delete o.x");
        assertValid(fn);
    }

    @Test
    void increment() {
        CfgFunction fn = compile("var x = 0; x++");
        assertValid(fn);
    }

    @Test
    void compoundAssignment() {
        CfgFunction fn = compile("o.x += 1");
        assertValid(fn);
    }

    // --- Exceptions ---

    @Test
    void tryCatch() {
        CfgFunction fn = compile("try { f() } catch(e) { g(e) }");
        assertValid(fn);
        assertFalse(fn.exceptionHandlers().isEmpty(), "Expected exception handlers");
    }

    @Test
    void tryFinally() {
        CfgFunction fn = compile("try { f() } finally { g() }");
        assertValid(fn);
        assertFalse(fn.exceptionHandlers().isEmpty());
    }

    @Test
    void tryCatchFinally() {
        CfgFunction fn = compile("try { f() } catch(e) { g() } finally { h() }");
        assertValid(fn);
    }

    @Test
    void throwStatement() {
        CfgFunction fn = compile("throw new Error('oops')");
        assertValid(fn);
    }

    // --- Iteration ---

    @Test
    void forIn() {
        CfgFunction fn = compile("for (var k in o) {}");
        assertValid(fn);
    }

    @Test
    void withStatement() {
        CfgFunction fn = compile("with(o) { x }");
        assertValid(fn);
    }

    // --- Generators ---

    @Test
    void generatorFunction() {
        CfgFunction fn = compile("function* g() { yield 1 }");
        assertValid(fn);
        assertEquals(1, fn.nestedFunctions().size());
        CfgFunction gen = fn.nestedFunctions().get(0);
        assertValid(gen);
        assertTrue(gen.isGenerator());
    }

    // --- Complex programs ---

    @Test
    void fibonacci() {
        CfgFunction fn =
                compile(
                        "function fib(n) {\n"
                                + "  if (n <= 1) return n;\n"
                                + "  return fib(n - 1) + fib(n - 2);\n"
                                + "}");
        assertValid(fn);
        assertEquals(1, fn.nestedFunctions().size());
        assertValid(fn.nestedFunctions().get(0));
    }

    @Test
    void switchStatement() {
        CfgFunction fn =
                compile(
                        "switch(x) {\n"
                                + "  case 1: y; break;\n"
                                + "  case 2: z; break;\n"
                                + "  default: w;\n"
                                + "}");
        assertValid(fn);
    }

    @Test
    void forLoop() {
        CfgFunction fn = compile("for (var i = 0; i < 10; i++) { f(i) }");
        assertValid(fn);
    }

    @Test
    void objectLiteral() {
        CfgFunction fn = compile("var o = {a: 1, b: 2}");
        assertValid(fn);
    }

    @Test
    void arrayLiteral() {
        CfgFunction fn = compile("var a = [1, 2, 3]");
        assertValid(fn);
    }

    @Test
    void nestedFunctions() {
        CfgFunction fn =
                compile(
                        "function outer() {\n"
                                + "  function inner() { return 1; }\n"
                                + "  return inner();\n"
                                + "}");
        assertValid(fn);
        assertEquals(1, fn.nestedFunctions().size());
        CfgFunction outer = fn.nestedFunctions().get(0);
        assertValid(outer);
        assertEquals(1, outer.nestedFunctions().size());
    }

    @Test
    void multipleStatements() {
        CfgFunction fn = compile("var x = 1; var y = 2; x + y");
        assertValid(fn);
    }

    @Test
    void stringConcat() {
        CfgFunction fn = compile("'hello' + ' ' + 'world'");
        assertValid(fn);
    }

    @Test
    void regexp() {
        CfgFunction fn = compile("/abc/g");
        assertValid(fn);
    }

    @Test
    void doWhile() {
        CfgFunction fn = compile("do { x } while (y)");
        assertValid(fn);
    }

    @Test
    void labeledBreak() {
        CfgFunction fn = compile("outer: for (;;) { break outer; }");
        assertValid(fn);
    }

    @Test
    void returnValue() {
        CfgFunction fn = compile("function f() { return 42; }");
        assertValid(fn);
        CfgFunction f = fn.nestedFunctions().get(0);
        assertValid(f);
    }

    @Test
    void emptyFunction() {
        CfgFunction fn = compile("function f() {}");
        assertValid(fn);
        CfgFunction f = fn.nestedFunctions().get(0);
        assertValid(f);
    }

    @Test
    void comparison() {
        CfgFunction fn = compile("1 < 2 && 3 >= 4");
        assertValid(fn);
    }

    @Test
    void strictEquality() {
        CfgFunction fn = compile("x === y");
        assertValid(fn);
    }

    @Test
    void instanceofExpression() {
        CfgFunction fn = compile("x instanceof Array");
        assertValid(fn);
    }

    @Test
    void inExpression() {
        CfgFunction fn = compile("'x' in o");
        assertValid(fn);
    }

    @Test
    void debuggerStatement() {
        CfgFunction fn = compile("debugger");
        assertValid(fn);
    }

    @Test
    void complexControlFlow() {
        CfgFunction fn =
                compile(
                        "function f(x) {\n"
                                + "  if (x > 0) {\n"
                                + "    while (x > 0) {\n"
                                + "      x--;\n"
                                + "      if (x === 5) break;\n"
                                + "    }\n"
                                + "    return x;\n"
                                + "  } else {\n"
                                + "    return -x;\n"
                                + "  }\n"
                                + "}");
        assertValid(fn);
        assertValid(fn.nestedFunctions().get(0));
    }
}
