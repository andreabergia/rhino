package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.testutils.Utils;

class ClassesTest {
    @Test
    void classAsStatementTypeofInstanceof() {
        String script =
                "class Foo { constructor(){} }\n"
                        + "var c = new Foo;\n"
                        + "typeof c === 'object' && c instanceof Foo";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void missingConstructorIsGenerated() {
        String script = "class Foo {}\nnew Foo() instanceof Foo;\n";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void cannotCallClassConstructorWithoutNew() {
        String script = "class Foo { constructor(){} }\nFoo()";
        EcmaError err =
                assertThrows(
                        EcmaError.class,
                        () -> Utils.executeScript(script, true)); // TODO: multiple modes
        assertEquals(
                "TypeError: Class constructor Foo cannot be invoked without new (myScript.js#2)",
                err.getMessage());
    }

    @Test
    void constructorBody() {
        String script =
                ""
                        + "class Square {\n"
                        + "  constructor(side){\n"
                        + "    this.side = side;\n"
                        + "  }\n"
                        + "}\n"
                        + "var c = new Square(42);\n"
                        + "c.side === 42";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void methods() {
        String script =
                "class Rectangle {\n"
                        + "  constructor(w, h) {\n"
                        + "   this.w = w;\n"
                        + "   this.h = h;\n"
                        + "  }\n"
                        + "  area() {\n"
                        + "   return this.w * this.h;\n"
                        + "  }\n"
                        + "}\n"
                        + "var c = new Rectangle(3, 2);\n"
                        + "c.area() === 6";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void staticFunctions() {
        String script =
                "class Dog {\n"
                        + "  constructor() {}\n"
                        + "  static bark() {\n"
                        + "   return 'woof';\n"
                        + "  }\n"
                        + "}\n"
                        + "Dog.bark() === 'woof'";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    // TODO:
    // - [X] auto generated constructor if missing
    // - [ ] var properties (all sort of keys, including symbols!)
    // - [ ] static var properties
    // - [ ] extends
    // - [ ] class expression
    // - [ ] name inference for class expression
    // - [ ] toString => sourceCodeProvider
    // - [ ] compiled mode
}
