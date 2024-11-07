package org.mozilla.javascript;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

public class SuperTest {
    // Lexer/Parser test cases

    @Test
    public void superIsNotAKeywordUntilES6() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_1_8);
            Script test = cx.compileString("var super = 42;", "test", 1, null);
            assertNotNull(test);
        }
    }

    @Test
    public void superIsAKeywordInES6AndCannotBeUsedAsVariableName() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            EvaluatorException err =
                    assertThrows(
                            EvaluatorException.class,
                            () -> cx.compileString("var super = 42;", "test", 1, null));
            assertEquals("missing variable name (test#1)", err.getMessage());
        }
    }

    @Test
    public void isSyntaxErrorIfHasDirectSuperOfMethodDefinitionIsTrue() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            EvaluatorException err =
                    assertThrows(
                            EvaluatorException.class,
                            () ->
                                    cx.compileString(
                                            "({ method() { super(); }});\n", "test", 1, null));
            assertEquals("super should be inside a shorthand function", err.getMessage());
        }
    }

    @Test
    public void superCannotBeUsedInAPropertyValue() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            EvaluatorException err =
                    assertThrows(
                            EvaluatorException.class,
                            () -> cx.compileString("var o = { a: super.b }", "test", 1, null));
            assertEquals("super should be inside a shorthand function (test#1)", err.getMessage());
        }
    }

    @Test
    public void superCannotBeUsedInAComputedPropertyName() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            EvaluatorException err =
                    assertThrows(
                            EvaluatorException.class,
                            () -> cx.compileString("var o = { [super.x]: 42 }", "test", 1, null));
            assertEquals("super should be inside a shorthand function (test#1)", err.getMessage());
        }
    }

    @Test
    public void superCannotBeUsedInANonShorthandMethod() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            EvaluatorException err =
                    assertThrows(
                            EvaluatorException.class,
                            () ->
                                    cx.compileString(
                                            "var o = { f: function() { super.x } }",
                                            "test",
                                            1,
                                            null));
            assertEquals("super should be inside a shorthand function (test#1)", err.getMessage());
        }
    }

    // Object prototype (no classes) cases

    @Test
    public void propertyAccessByName() throws Exception {
        String script =
                ""
                        + "const a = { x: 1 };\n"
                        + "const b = {\n"
                        + "  f() {\n"
                        + "    return super.x;\n"
                        + "  }\n"
                        + "};\n"
                        + "\n"
                        + "Object.setPrototypeOf(b, a);\n"
                        + "b.f();";
        Utils.assertWithAllOptimizationLevelsES6(1, script);
    }

    @Test
    public void propertyAccessIgnoresThisAndUsesDeclaringObject() throws Exception {
        String script =
                ""
                        + "const a = { x: 1 };\n"
                        + "const b = {\n"
                        + "  f() {\n"
                        + "    return super.x;\n"
                        + "  }\n"
                        + "};\n"
                        + "\n"
                        + "Object.setPrototypeOf(b, a);\n"
                        + "var fn = b.f;\n"
                        + "fn()\n";
        Utils.assertWithAllOptimizationLevelsES6(1, script);
    }

    @Test
    public void propertyAccessIgnoresThisAndUsesDeclaringObject2() throws Exception {
        String script =
                ""
                        + "const a = { x: 1 };\n"
                        + "const b = {\n"
                        + "  f() {\n"
                        + "    return super.x;\n"
                        + "  }\n"
                        + "};\n"
                        + "\n"
                        + "Object.setPrototypeOf(b, a);\n"
                        + "var fn = b.f;\n"
                        + "var c = { __proto__: {x: 42}, fn };"
                        + "c.fn()\n";
        Utils.assertWithAllOptimizationLevelsES6(1, script);
    }

    //	@Test
    //	public void functionDefaultArgsBasic() throws Exception {
    //		final String script = "const obj1 = {\n" +
    //				"  method1() {\n" +
    //				"    \"method 1\";\n" +
    //				"  },\n" +
    //				"};\n" +
    //				"\n" +
    //				"const obj2 = {\n" +
    //				"  method2() {\n" +
    //				"    super.method1();\n" +
    //				"  },\n" +
    //				"};\n" +
    //				"\n" +
    //				"Object.setPrototypeOf(obj2, obj1);\n" +
    //				"obj2.method2();";
    //		Utils.assertWithAllOptimizationLevelsES6("method 1", script);
    //	}
}
