package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.testutils.Utils;

class ClassesTest {
    @Test
    void classAsStatementTypeofInstanceof() {
        String script =
                "class Foo { constructor(){} }\n"
                        + "var c = new Foo;\n"
                        + "(typeof c === 'object') + ':' + (c instanceof Foo)";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("true:true", res);
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
                        + "var r = new Rectangle(3, 2);\n"
                        + "var desc = Object.getOwnPropertyDescriptor(Rectangle.prototype, 'area');\n"
                        + "r.area() + ':' + (desc.value === r.area) + ':' + desc.configurable + ':' + desc.enumerable";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("6:true:true:false", res);
    }

    @Test
    void methodGenerators() {
        String script =
                "class C {\n"
                        + "  *g() {\n"
                        + "   yield 1;\n"
                        + "   yield 2;\n"
                        + "  }\n"
                        + "}\n"
                        + "var g = new C().g();\n"
                        + "var v1 = g.next();\n"
                        + "var v2 = g.next();\n"
                        + "var v3 = g.next();\n"
                        + "v1.value + '-' + v1.done + ':' + v2.value + '-' + v2.done + ':' + v3.value + '-' + v3.done";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("1-false:2-false:undefined-true", res);
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
                        + "        this._size = value;\n"
                        + "    }\n"
                        + "\n"
                        + "    get size() {\n"
                        + "        return this._size;\n"
                        + "    }\n"
                        + "}\n"
                        + "var s1 = new Store();\n"
                        + "s1.size = 42;\n"
                        + "var propDesc = Object.getOwnPropertyDescriptor(Store.prototype, 'size');\n"
                        + "(s1.size === 42) + ':' + "
                        + "propDesc.configurable + ':' + "
                        + "(propDesc.get != null) + ':' + "
                        + "(propDesc.set != null) + ':' + "
                        + "propDesc.enumerable";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("true:true:true:true:false", res);
    }

    @Test
    void setterThenGetterWorksToo() {
        String script =
                "class Store {\n"
                        + "    get size() {\n"
                        + "        return this._size;\n"
                        + "    }\n"
                        + "    set size(value) {\n"
                        + "        this._size = value;\n"
                        + "    }\n"
                        + "}\n"
                        + "var s1 = new Store();\n"
                        + "s1.size = 42;\n"
                        + "var propDesc = Object.getOwnPropertyDescriptor(Store.prototype, 'size');\n"
                        + "(s1.size === 42) + ':' + "
                        + "propDesc.configurable + ':' + "
                        + "(propDesc.get != null) + ':' + "
                        + "(propDesc.set != null) + ':' + "
                        + "propDesc.enumerable";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("true:true:true:true:false", res);
    }

    @Test
    void getterOnlyWorks() {
        String script =
                "class Store {\n"
                        + "    get size() {\n"
                        + "        return this._size;\n"
                        + "    }\n"
                        + "}\n"
                        + "var s1 = new Store();\n"
                        + "s1._size = 42;\n"
                        + "var propDesc = Object.getOwnPropertyDescriptor(Store.prototype, 'size');\n"
                        + "(s1.size === 42) + ':' + "
                        + "propDesc.configurable + ':' + "
                        + "(propDesc.get != null) + ':' + "
                        + "(propDesc.set != null) + ':' + "
                        + "propDesc.enumerable";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("true:true:true:false:false", res);
    }

    @Test
    void setterOnlyWork() {
        String script =
                "class Store {\n"
                        + "    set size(value) {\n"
                        + "        this._size = value;\n"
                        + "    }\n"
                        + "\n"
                        + "}\n"
                        + "var s1 = new Store();\n"
                        + "s1.size = 42;\n"
                        + "var propDesc = Object.getOwnPropertyDescriptor(Store.prototype, 'size');\n"
                        + "(s1._size === 42) + ':' + "
                        + "propDesc.configurable + ':' + "
                        + "(propDesc.get != null) + ':' + "
                        + "(propDesc.set != null) + ':' + "
                        + "propDesc.enumerable";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("true:true:false:true:false", res);
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
                        + "var propDesc = Object.getOwnPropertyDescriptor(Store, 'size');\n"
                        + "(Store.size === 3) + ':' + "
                        + "propDesc.configurable + ':' + "
                        + "(propDesc.get != null) + ':' + "
                        + "(propDesc.set != null) + ':' + "
                        + "propDesc.enumerable";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("true:true:true:true:false", res);
    }

    @Test
    void basicProperties() {
        String script =
                "class Cat { cute = 'yes'; }\n"
                        + "var desc = Object.getOwnPropertyDescriptor(new Cat, 'cute');\n"
                        + "new Cat().cute + ':' + "
                        + "desc.value + ':' +"
                        + "desc.get + ':' + "
                        + "desc.set + ':' + "
                        + "desc.configurable + ':' + "
                        + "desc.enumerable";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("yes:yes:undefined:undefined:true:true", res);
    }

    @Test
    @Disabled("we need Token.UNDEFINED")
    void propertyWithoutInitializer() {
        String script =
                "class A { p; }\n"
                        + "var desc = Object.getOwnPropertyDescriptor(new A, 'p');\n"
                        + "desc.value + ':' +"
                        + "desc.get + ':' + "
                        + "desc.set + ':' + "
                        + "desc.configurable + ':' + "
                        + "desc.enumerable";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("undefined:undefined:undefined:true:true", res);
    }

    @Test
    void propertyDeclarationAndConstructorWithBody() {
        String script =
                "class Cat {\n"
                        + "   cute = 42;\n"
                        + "   constructor(name) { this.name = name; }\n"
                        + "}\n"
                        + "var johnny = new Cat('Johnny');\n"
                        + "johnny.name + johnny.cute";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("Johnny42", res);
    }

    @Test
    void propertyValueIsRegExp() {
        // In the interpreter, regexp literals are stored in the constant pool; this checks that
        // everything in the CodeGenerator handles the relationship correctly
        String script = "class A { re = /abc/; }\nnew A().re.toString()";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("/abc/", res);
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
                        + "new T().index + ':' + new T().index + ':' + counter";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("0:1:2", res);
    }

    @Test
    void propertyWithComputedName() {
        String script =
                "function k(x) { return 'k' + x; }\n"
                        + "class Cat { [k(0)] = true; }\n"
                        + "new Cat().k0";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void staticPropertyWithComputedName() {
        String script =
                "function k(x) { return 'k' + x; }\n"
                        + "class Cat { static [k(0)] = true; }\n"
                        + "Cat.k0";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void propertyWithComputedNameInt() {
        String script =
                "function dbl(x) { return 2 * x; }\n"
                        + "class Cat { [dbl(1)] = true; }\n"
                        + "new Cat()['2']";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void staticPropertyWithComputedNameInt() {
        String script =
                "function dbl(x) { return 2 * x; }\n"
                        + "class Cat { static [dbl(1)] = true; }\n"
                        + "Cat['2']";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void propertyWithQuotedNames() {
        String script = "class Cat { 'are u cute' = true; }\nnew Cat()['are u cute']";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void propertyValueIsFunction() {
        String script =
                "class Cat { name = function() { return 'fluffy'; } }\n" + "new Cat().name()";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("fluffy", res);
    }

    @Test
    void propertyValueIsFunctionHandlesThisCorrectly() {
        String script =
                "class Cat {\n"
                        + "   constructor(age) { this.age = age; }\n"
                        + "   years = function() { return this.age + ' years'; }\n"
                        + "}\n"
                        + "new Cat(42).years()";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("42 years", res);
    }

    @Test
    void propertyValueIsLambdaAndHandlesThisCorrectly() {
        String script =
                "class Cat {\n"
                        + "   constructor(age) { this.age = age; }\n"
                        + "   years = () => this.age + ' years';\n"
                        + "}\n"
                        + "new Cat(42).years()";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("42 years", res);
    }

    @Test
    void basicStaticProperties() {
        String script = "class Cat { static cute = true; }\nCat.cute";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

    @Test
    void classesAsExpression() {
        String script = "var C = class C {}\n" + "typeof new C() + ':' + C.name";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("object:C", res);
    }

    @Test
    void classesAsExpressionInferName() {
        String script = "var C = class {}\n" + "typeof new C() + ':' + C.name";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("object:C", res);
    }

    @Test
    void inferredNameShadowsDefinedName() {
        String script = "var D = class C {}\n" + "typeof D + ':' + typeof C + ':' + D.name";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("function:undefined:C", res);
    }

    @Test
    void methodsWithSymbolName() {
        String script =
                "var s = Symbol();\n"
                        + "class C {\n"
                        + "  [s]() { return 'sym'; }\n"
                        + "}\n"
                        + "new C()[s]()";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("sym", res);
    }

    @Test
    void basicExtendsHasCorrectPrototypes() {
        String script =
                "class A {}\n"
                        + "class B extends A {}\n"
                        + "(Object.getPrototypeOf(B) === A) + ':' + "
                        + "(Object.getPrototypeOf(B.prototype) === A.prototype) + ':' + "
                        + "(new B instanceof A)";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("true:true:true", res);
    }

    @Test
    void extendsWorksAsExpected() {
        String script =
                "class A {\n"
                        + "  static as = 'as';\n"
                        + "  am() { return 'am'; }\n"
                        + "}\n"
                        + "class B extends A {"
                        + "  static bs = 'bs';\n"
                        + "  bm() { return 'bm'; }\n"
                        + "}\n"
                        + "var b = new B();\n"
                        + "b.am() + ':' + b.bm() + ':' + B.as + ':' + B.bs";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("am:bm:as:bs", res);
    }

    @Test
    void extendsExpressions() {
        String script =
                "class B extends class {\n"
                        + "  a() { return 'a'; }\n"
                        + "}\n"
                        + "{"
                        + "  b() { return 'b'; }\n"
                        + "}\n"
                        + "var b = new B();\n"
                        + "b.a() + ':' + b.b()";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("a:b", res);
    }

    @Test
    void extendsNotConstructor() {
        String script = "class B extends 42 {}";
        EcmaError err =
                assertThrows(
                        EcmaError.class,
                        () -> Utils.executeScript(script, true)); // TODO: multiple modes
        assertEquals(
                // TODO
                //				"TypeError: Class extends value 42 is not a constructor or null
                // (myScript.js#2)",
                "TypeError: Class extends value 42 is not a constructor or null", err.getMessage());
    }

    @Test
    void extendsProtoParentNotObject() {
        String script = "function A() {}\nA.prototype = 42;\nclass B extends A {}";
        EcmaError err =
                assertThrows(
                        EcmaError.class,
                        () -> Utils.executeScript(script, true)); // TODO: multiple modes
        assertEquals(
                // TODO
                //				"TypeError: Class extends value does not have valid prototype property 42
                // (myScript.js#3)",
                "TypeError: Class extends value does not have valid prototype property 42 (myScript.js#2)",
                err.getMessage());
    }

    @Test
    void superInMethod() {
        String script =
                "class Base {\n"
                        + "  f() {\n"
                        + "    return 'base';\n"
                        + "  }\n"
                        + "}\n"
                        + "class Derived extends Base {\n"
                        + "  f() {\n"
                        + "    return super.f() + ':derived' ;\n"
                        + "  }\n"
                        + "}\n"
                        + "new Derived().f()\n";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("base:derived", res);
    }

    @Test
    @Disabled("Requires implementing super() in the generated constructor")
    void superBehavesInNonObviousWay() {
        String script =
                "class Base {\n"
                        + "\tconstructor() {\n"
                        + "\t\tthis.x = 1;\n"
                        + "\t}\n"
                        + "}\n"
                        + "class Derived extends Base {\n"
                        + "\tf() {\n"
                        + "\t\treturn super.x;\n"
                        + "\t}\n"
                        + "}\n"
                        + "var d = new Derived();\n"
                        + "var result = d.x + ':' + d.f();\n"
                        + "Base.prototype.x = 2;\n"
                        + "result += ':' + d.x + ':' + d.f();\n";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals("1:undefined:1:2", res);
    }

    // TODO:
    // - [X] auto generated constructor if missing
    // - [X] getter/setter (non static)
    // - [X] getter/setter (static)
    // - [x] var properties (all sort of keys, including symbols!)
    // - [x] class expression
    // - [x] name inference for class expression
    // - [x] static properties
    // - [x] property without initializer value
    // - [x] basic extends
    // - [ ] implicit super call in constructor
    // - [ ] explicit super call in constructor
    // - [ ] super access in methods (I have no idea how to treat the home object!)
    // - [ ] duplicate property names
    // - [ ] do not generate method name assignment
    // - [ ] self-referring inside the class body (??)
    // - [ ] create new function for the initialization of properties (specified in 15.7.10 of the
    // spec). Test that we are not capturing constructor's arguments (i.e. x = x; constructor(x))
    // should not work
    // - [ ] line and column numbers
    // - [ ] toString => sourceCodeProvider
    // - [ ] compiled mode
}
