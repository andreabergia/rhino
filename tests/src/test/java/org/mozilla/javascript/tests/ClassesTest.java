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
    void typeofClassIsFunction() {
        String script = "class Foo {}\ntypeof Foo === 'function'";
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

    @Test
    void prototypeOfClassIsFunctionPrototype() {
        String script = "class Foo {}\nObject.getPrototypeOf(Foo) === Function.prototype";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void constructorPropertySetCorrect() {
        String script = "class Foo {}\nFoo.constructor === Function";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void prototypePropertyHasConstructorPropertySetCorrect() {
        String script = "class Foo {}\nFoo.prototype.constructor === Foo";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void getterSetterWork() {
        String script =
                "class Store {\n"
                        + "    set size(value) {\n"
                        + "        if (value > 0) {\n"
                        + "            this._size = value;\n"
                        + "        } else {\n"
                        + "            this._size = 0;\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    get size() {\n"
                        + "        return this._size;\n"
                        + "    }\n"
                        + "}\n"
                        + "var s1 = new Store();\n"
                        + "s1.size = 42;\n"
                        + "var s2 = new Store();\n"
                        + "s2.size = -1;\n"
                        + "s1.size === 42 && s2.size === 0 && "
                        + "Object.getOwnPropertyDescriptor(Store.prototype, 'size') !== undefined";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void staticGetterSetterWork() {
        String script =
                "class Store {\n"
                        + "    static set size(value) {\n"
                        + "        Store._size = value * 2;\n"
                        + "    }\n"
                        + "\n"
                        + "    static get size() {\n"
                        + "        return Store._size + 1;\n"
                        + "    }\n"
                        + "}\n"
                        + "Store.size = 1;\n"
                        + "Store.size === 3";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void basicProps() {
        String script = "class Cat { cute = true; }\nnew Cat().cute";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void propValuesAreEvaluatedDuringConstructor() {
        String script =
                "var counter = 0;\n"
                        + "function next() {\n"
                        + "  return counter++;\n"
                        + "}\n"
                        + "class T {\n"
                        + "  index = next();\n"
                        + "}\n"
                        + "new T().index === 0 && new T().index === 1 && counter === 2";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void computedProps() {
        String script =
                "function k(x) { return 'k' + x; }\n"
                        + "class Cat { [k(0)] = true; }\n"
                        + "new Cat().k0";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void quotedNames() {
        String script = "class Cat { 'are u cute' = true; }\nnew Cat()['are u cute']";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void classesAsExpression() {
        String script = "var C = class C {}\ntypeof new C() === 'object'";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    // TODO:
    // - [X] auto generated constructor if missing
    // - [X] getter/setter (non static)
    // - [X] getter/setter (static)
    // - [x] var properties (all sort of keys, including symbols!)
    // - [ ] class expression
    // - [ ] extends
    //       note: set home object for methods
    // - [ ] name inference for class expression
    // - [ ] toString => sourceCodeProvider
    // - [ ] compiled mode
    // - [ ] static properties
}
