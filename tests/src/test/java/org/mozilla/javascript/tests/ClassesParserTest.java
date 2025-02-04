package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mozilla.javascript.tests.ParserLineColumnNumberTest.assertLineColumnAre;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.ClassDefNode;
import org.mozilla.javascript.ast.ClassProperty;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.ComputedPropertyKey;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.GeneratorMethodDefinition;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

class ClassesParserTest {
    private CompilerEnvirons environment;

    @BeforeEach
    public void setUp() throws Exception {
        environment = new CompilerEnvirons();
        environment.setLanguageVersion(Context.VERSION_ES6);
    }

    private AstRoot parse(String source) {
        return ParserTest.parse(source, null, null, true, environment);
    }

    private void assertThrowsParseError(String source, String expectedError) {
        ParserTest.parse(source, new String[] {expectedError}, null, true, environment);
    }

    @Test
    public void basicDeclaration() {
        AstRoot root = parse("class Rectangle {}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertLineColumnAre(classDefNode, 0, 1);
        assertNull(classDefNode.getExtendsNode());
        assertName(classDefNode.getClassName(), "Rectangle", 0, 7);
        assertNull(classDefNode.getConstructor());
        assertTrue(classDefNode.getProperties().isEmpty());
    }

    @Test
    public void classWithoutName() {
        AstRoot root = parse("class {}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertNull(classDefNode.getClassName());
        assertNull(classDefNode.getConstructor());
    }

    @Test
    public void classInExpression() {
        AstRoot root = parse("var x = class {}");

        VariableDeclaration varStatement =
                assertInstanceOf(VariableDeclaration.class, root.getFirstChild());
        assertEquals(1, varStatement.getVariables().size());

        VariableInitializer var = varStatement.getVariables().get(0);
        assertName(var.getTarget(), "x", 0, 5);

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, var.getInitializer());
        assertLineColumnAre(classDefNode, 0, 9);
        assertNull(classDefNode.getClassName());
        assertNull(classDefNode.getConstructor());
    }

    @Test
    public void classExtendsName() {
        AstRoot root = parse("class Dog extends Animal {}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertName(classDefNode.getClassName(), "Dog", 0, 7);

        AstNode extendsNode = classDefNode.getExtendsNode();
        assertNotNull(extendsNode);
        assertName(extendsNode, "Animal", 0, 19);
    }

    @Test
    public void classExtendsExpression() {
        AstRoot root = parse("class Dog extends class Animal {} {}");

        ClassDefNode dog = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertName(dog.getClassName(), "Dog", 0, 7);

        AstNode extendsNode = dog.getExtendsNode();
        assertNotNull(extendsNode);
        ClassDefNode animal = assertInstanceOf(ClassDefNode.class, extendsNode);
        assertName(animal.getClassName(), "Animal", 0, 25);
    }

    @Test
    public void classConstructor() {
        AstRoot root =
                parse(
                        "class Rectangle {\n"
                                + "  /** the class constructor */\n"
                                + "  constructor(w, h = 1) {}"
                                + "}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertName(classDefNode.getClassName(), "Rectangle", 0, 7);

        FunctionNode constructor =
                assertInstanceOf(FunctionNode.class, classDefNode.getConstructor());
        assertLineColumnAre(constructor, 2, 3);
        assertEquals(FunctionNode.CONSTRUCTOR_FUNCTION, constructor.getFunctionType());
        assertEquals(
                List.of("w", "h"),
                constructor.getParams().stream()
                        .map(AstNode::toSource)
                        .collect(Collectors.toList()));
        assertInstanceOf(Block.class, constructor.getBody());
        Comment jsDocNode = constructor.getJsDocNode();
        assertNotNull(jsDocNode);
        assertLineColumnAre(jsDocNode, 1, 3);
        assertEquals("/** the class constructor */", jsDocNode.getValue());
    }

    @Test
    public void constructorCanCallSuperIfExtendingSomething() {
        AstRoot root =
                parse(
                        "class X extends Object {\n"
                                + "  constructor() {\n"
                                + "    super();\n"
                                + "  }\n"
                                + "}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        FunctionNode constructor =
                assertInstanceOf(FunctionNode.class, classDefNode.getConstructor());
        Block block = assertInstanceOf(Block.class, constructor.getBody());
        ExpressionStatement firstStatement =
                assertInstanceOf(ExpressionStatement.class, block.getFirstChild());
        FunctionCall superCall =
                assertInstanceOf(FunctionCall.class, firstStatement.getExpression());
        KeywordLiteral target = assertInstanceOf(KeywordLiteral.class, superCall.getTarget());
        assertEquals(Token.SUPER, target.getType());
    }

    @Test
    public void classesCanHaveOnlyOneConstructor() {
        assertThrowsParseError(
                "class Rectangle {\n"
                        + "  constructor() { console.log('foo'); }\n"
                        + "  constructor() {}\n"
                        + "}",
                "duplicate constructor definition");
    }

    @Test
    public void constructorMustBeMethod() {
        assertThrowsParseError(
                "class Rectangle { constructor = function() {} }",
                "invalid constructor definition");
    }

    @Test
    public void constructorShouldNotBeProperties() {
        assertThrowsParseError(
                "class Rectangle { constructor = 42 }", "invalid constructor definition");
    }

    @Test
    public void constructorShouldNotBeGenerators() {
        assertThrowsParseError(
                "class Rectangle { *constructor() {} }", "invalid constructor definition");
    }

    @Test
    public void constructorShouldNotBeStatic() {
        assertThrowsParseError(
                "class Rectangle { static constructor() {} }", "constructor cannot be static");
    }

    @Test
    public void constructorShouldNotBeGetter() {
        assertThrowsParseError("class Rectangle { get constructor() {} }", "invalid property id");
    }

    @Test
    public void constructorShouldNotBeSetter() {
        assertThrowsParseError("class Rectangle { set constructor() {} }", "invalid property id");
    }

    @Test
    public void superCannotBeCalledOutsideOfConstructor() {
        assertThrowsParseError(
                "class X extends Object { f() { super(); } }", "invalid call to super()");
    }

    @Test
    public void superCannotBeCalledIfNotExtendingAnything() {
        assertThrowsParseError("class X { constructor() { super(); } }", "invalid call to super()");
    }

    @Test
    public void propertiesCanBeDefined() {
        AstRoot root = parse("class Rectangle {\n  x;\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsStandardProperty(prop, "x", 1, 3);
        assertNull(prop.getValue());
        assertEquals("  x", prop.toSource(0));
    }

    @Test
    public void propertiesCannotUseColon() {
        assertThrowsParseError(
                "class Rectangle { x: 42 }", "missing ( before function parameters.");
    }

    @Test
    public void propertiesCanBeStatic() {
        AstRoot root = parse("class Rectangle { static x; }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty prop = getOnlyProp(classDefNode);
        assertLineColumnAre(prop, 0, 19);
        assertName(prop.getKey(), "x", 0, 26);
        assertTrue(prop.isStatic());
        assertNull(prop.getValue());
        assertEquals("  static x", prop.toSource(0));
    }

    @Test
    public void staticCanBeOnDifferentLine() {
        AstRoot root = parse("class X {\nstatic\nx; }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty prop = getOnlyProp(classDefNode);
        assertLineColumnAre(prop, 1, 1);
        assertName(prop.getKey(), "x", 2, 1);
        assertTrue(prop.isStatic());
        assertEquals("  static x", prop.toSource(0));
    }

    @Test
    public void propertiesCanHaveCommentsBetweenStaticAndPropName() {
        AstRoot root = parse("class Rectangle { static /* comment */ x; }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty prop = getOnlyProp(classDefNode);
        assertLineColumnAre(prop, 0, 19);
        assertName(prop.getKey(), "x", 0, 40);
        assertTrue(prop.isStatic());
        assertEquals("  static x", prop.toSource(0));
    }

    @Test
    public void multiplePropertiesCanBeOnSameLine() {
        AstRoot root = parse("class Rectangle { x; y; }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

        List<ClassProperty> properties = classDefNode.getProperties();
        assertEquals(2, properties.size());
        assertIsStandardProperty(properties.get(0), "x", 0, 19);
        assertIsStandardProperty(properties.get(1), "y", 0, 22);
    }

    @Test
    public void commentsAreNotValidSeparatorBetweenProperties() {
        assertThrowsParseError(
                "class Rectangle { x /* comment */ y }", "missing ( before function parameters.");
    }

    @Test
    public void propertiesCanBeSeparatedByNewLineWithoutSemicolon() {
        AstRoot root = parse("class Rectangle {\n  x\ny\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

        List<ClassProperty> properties = classDefNode.getProperties();
        assertEquals(2, properties.size());
        assertIsStandardProperty(properties.get(0), "x", 1, 3);
        assertIsStandardProperty(properties.get(1), "y", 2, 1);
    }

    @Test
    public void propertiesShouldBeSeparatedByNewlineOrSemicolon() {
        assertThrowsParseError("class X { a b }", "missing ( before function parameters.");
    }

    @Test
    public void propertiesCanHaveJsDoc() {
        AstRoot root = parse("class Rectangle {\n  /** documentation */ y;\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty prop = getOnlyProp(classDefNode);
        assertLineColumnAre(prop, 1, 24);
        assertNull(prop.getJsDoc());
        Name name = assertName(prop.getKey(), "y", 1, 24);
        assertEquals("/** documentation */", name.getJsDoc());
        assertEquals("  y", prop.toSource(0));
    }

    @Test
    public void propertiesCanBeInitialized() {
        AstRoot root = parse("class Rectangle {\n  x = 0;\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty prop = getOnlyProp(classDefNode);
        assertName(prop.getKey(), "x", 1, 3);
        assertFalse(prop.isStatic());

        NumberLiteral value = assertInstanceOf(NumberLiteral.class, prop.getValue());
        assertEquals("0", value.getValue());
        assertLineColumnAre(value, 1, 7);
        assertEquals("  x = 0", prop.toSource(0));
    }

    @Test
    public void propertiesCanBeInitializedWithAFunctionButAreNotMethods() {
        AstRoot root = parse("class Rectangle {\n  x = function() {};\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

        ClassProperty prop = getOnlyProp(classDefNode);
        assertLineColumnAre(prop, 1, 3);
        assertFalse(prop.isStatic());
        assertFalse(prop.isMethod());
        FunctionNode value = assertInstanceOf(FunctionNode.class, prop.getValue());
        assertLineColumnAre(value, 1, 7);
        assertEquals("  x = function() {\n}", prop.toSource(0));
    }

    @Test
    public void propertiesCanBeStaticAndInitialized() {
        AstRoot root = parse("class Rectangle {\n  static x = 0;\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty prop = getOnlyProp(classDefNode);
        assertTrue(prop.isStatic());
        assertLineColumnAre(prop, 1, 3);
        NumberLiteral value = assertInstanceOf(NumberLiteral.class, prop.getValue());
        assertEquals("0", value.getValue());
        assertLineColumnAre(value, 1, 14);
        assertEquals("  static x = 0", prop.toSource(0));
    }

    @Test
    public void propertyNameCanBeLiteralNumber() {
        AstRoot root = parse("class X { 42 }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty prop = getOnlyProp(classDefNode);
        NumberLiteral literal = assertInstanceOf(NumberLiteral.class, prop.getKey());
        assertEquals("42", literal.getValue());
        assertLineColumnAre(literal, 0, 11);
        assertEquals("  42", prop.toSource(0));
    }

    @Test
    public void propertyNameCanBeLiteralString() {
        AstRoot root = parse("class X { \"a\" }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertEquals("X", classDefNode.getClassName().getIdentifier());

        ClassProperty prop = getOnlyProp(classDefNode);
        StringLiteral literal = assertInstanceOf(StringLiteral.class, prop.getKey());
        assertEquals("a", literal.getValue());
        assertLineColumnAre(literal, 0, 11);
        assertEquals("  \"a\"", prop.toSource(0));
    }

    @Test
    public void propertyNameCanBeLiteralBoolean() {
        AstRoot root = parse("class X { true = 0; }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertEquals("X", classDefNode.getClassName().getIdentifier());

        ClassProperty prop = getOnlyProp(classDefNode);
        Name literal = assertInstanceOf(Name.class, prop.getKey());
        assertEquals("true", literal.getIdentifier());
        assertLineColumnAre(literal, 0, 11);
        assertEquals("  true = 0", prop.toSource(0));
    }

    @Test
    public void propertyNameCanBeExpressions() {
        AstRoot root = parse("class X { [1 +  2] }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertEquals("X", classDefNode.getClassName().getIdentifier());

        ClassProperty prop = getOnlyProp(classDefNode);
        ComputedPropertyKey key = assertInstanceOf(ComputedPropertyKey.class, prop.getKey());
        assertLineColumnAre(key, 0, 11);
        assertInstanceOf(InfixExpression.class, key.getExpression());
        assertEquals("  [1 + 2]", prop.toSource(0));
    }

    // Special case of the above test
    @Test
    public void propertyNameCanBeSymbol() {
        AstRoot root = parse("class X { [Symbol.isInstance] = () => false }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertEquals("X", classDefNode.getClassName().getIdentifier());

        ClassProperty prop = getOnlyProp(classDefNode);
        ComputedPropertyKey key = assertInstanceOf(ComputedPropertyKey.class, prop.getKey());
        assertLineColumnAre(key, 0, 11);
        assertInstanceOf(InfixExpression.class, key.getExpression());
        assertEquals("  [Symbol.isInstance] = () => false", prop.toSource(0));
    }

    @Test
    public void propertyNamesCanBeDuplicated() {
        AstRoot root = parse("class X {\n  x;\n  x = 1; }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertEquals("X", classDefNode.getClassName().getIdentifier());

        List<ClassProperty> properties = classDefNode.getProperties();
        assertEquals(2, properties.size());

        ClassProperty prop = properties.get(0);
        assertName(prop.getKey(), "x", 1, 3);
        assertNull(prop.getValue());

        prop = properties.get(1);
        assertName(prop.getKey(), "x", 2, 3);
        NumberLiteral value = assertInstanceOf(NumberLiteral.class, prop.getValue());
        assertEquals("1", value.getValue());
    }

    @Test
    public void commentsAreAllowedInProperties() {
        AstRoot root =
                parse(
                        "class Rectangle {\n"
                                + "  /* one */ x /* two */ = /* three */ 42;\n"
                                + "  /* four */ y /* five */ ; /* six */\n"
                                + " /* seven */ }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        List<ClassProperty> properties = classDefNode.getProperties();
        assertEquals(2, properties.size());
        assertIsStandardProperty(properties.get(0), "x", 1, 13);
        assertIsStandardProperty(properties.get(1), "y", 2, 14);
    }

    @Test
    public void classesCanHaveMethods() {
        AstRoot root = parse("class X {\n  foo() {}\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertEquals("X", classDefNode.getClassName().getIdentifier());

        ClassProperty foo = getOnlyProp(classDefNode);
        assertIsMethod(foo, "foo", 1, 3);
        assertEquals("  foo() {\n  }\n", foo.toSource(0));
    }

    @Test
    public void methodsCanBeStatic() {
        AstRoot root = parse("class X {\n  static foo() {}\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertEquals("X", classDefNode.getClassName().getIdentifier());

        ClassProperty foo = getOnlyProp(classDefNode);
        assertIsStaticMethod(foo, "foo", 1, 3, 1, 10);
        assertEquals("  static foo() {\n  }\n", foo.toSource(0));
    }

    @Test
    public void semicolonOrNewLineIsOptionalAfterMethod() {
        AstRoot root = parse("class X{ a(){}b(){}; c() {}\nstatic d() {} }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        List<ClassProperty> properties = classDefNode.getProperties();
        assertEquals(4, properties.size());

        assertIsMethod(properties.get(0), "a", 0, 10);
        assertIsMethod(properties.get(1), "b", 0, 15);
        assertIsMethod(properties.get(2), "c", 0, 22);
        assertIsStaticMethod(properties.get(3), "d", 1, 1, 1, 8);
    }

    @Test
    public void commentsAreAllowedInMethods() {
        AstRoot root =
                parse(
                        "class Rectangle {\n"
                                + "  /* one */ f /* two */ ( /* three */ ) /* four */ { /* five */ };\n"
                                + "}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsMethod(prop, "f", 1, 13);
    }

    @Test
    public void methodNameCanBeSymbols() {
        AstRoot root = parse("class X { [Symbol.isInstance]() { return false; } }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        assertEquals("X", classDefNode.getClassName().getIdentifier());

        ClassProperty prop = getOnlyProp(classDefNode);
        ComputedPropertyKey key = assertInstanceOf(ComputedPropertyKey.class, prop.getKey());
        assertLineColumnAre(key, 0, 11);
        assertInstanceOf(InfixExpression.class, key.getExpression());
        assertEquals(
                "  [Symbol.isInstance]() {\n" + "    return false;\n" + "  }\n", prop.toSource(0));
    }

    @Test
    public void propertiesCanHaveGetterAndSetter() {
        AstRoot root =
                parse(
                        "class Rectangle {\n"
                                + "  get x() { return 42; }\n"
                                + "  set x(value) { /* ignored */ }\n"
                                + "}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        List<ClassProperty> properties = classDefNode.getProperties();
        assertEquals(2, properties.size());
        ClassProperty getter = properties.get(0);
        ClassProperty setter = properties.get(1);
        assertIsGetter(getter, "x", 1, 3, 1, 7, false);
        assertIsSetter(setter, "x", 2, 3, 2, 7, false);
        assertEquals("  get x() {\n    return 42;\n  }\n", getter.toSource(0));
        assertEquals("  set x(value) {\n    /* ignored */\n\n  }\n", setter.toSource(0));
    }

    @Test
    public void staticPropertiesCanHaveGetterAndSetter() {
        AstRoot root =
                parse(
                        "class Rectangle {\n"
                                + "  static get x() { return 42; }\n"
                                + "  static set x(value) { /* ignored */ }\n"
                                + "}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        List<ClassProperty> properties = classDefNode.getProperties();
        assertEquals(2, properties.size());
        assertIsGetter(properties.get(0), "x", 1, 3, 1, 14, true);
        assertIsSetter(properties.get(1), "x", 2, 3, 2, 14, true);
    }

    @Test
    public void commentsAreAllowedInGetter() {
        AstRoot root =
                parse(
                        "class Rectangle {\n"
                                + "  get /* one */ x /* two */ ( /* three */ ) /* four */ { /* five */ }\n"
                                + "}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty getter = getOnlyProp(classDefNode);
        assertIsGetter(getter, "x", 1, 3, 1, 17, false);
    }

    @Test
    public void getAndNameCanBeSeparatedByNewline() {
        AstRoot root = parse("class Rectangle {\nget\ny() { return 42; }\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty getter = getOnlyProp(classDefNode);
        assertIsGetter(getter, "y", 1, 1, 2, 1, false);
        assertEquals("  get y() {\n    return 42;\n  }\n", getter.toSource(0));
    }

    @Test
    public void setAndNameCanBeSeparatedByNewline() {
        AstRoot root = parse("class Rectangle {\nset\ny(value) { this._y = value; }\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());

        ClassProperty setter = getOnlyProp(classDefNode);
        assertIsSetter(setter, "y", 1, 1, 2, 1, false);
        assertEquals("  set y(value) {\n    this._y = value;\n  }\n", setter.toSource(0));
    }

    @Test
    public void getIsAValidPropertyName() {
        AstRoot root = parse("class Rectangle {\nget\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsStandardProperty(prop, "get", 1, 1);
    }

    @Test
    public void setIsAValidPropertyName() {
        AstRoot root = parse("class Rectangle {\nset =\n1}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsStandardProperty(prop, "set", 1, 1);
    }

    @Test
    public void methodsCanBeGenerators() {
        AstRoot root = parse("class X {\n*g() {} }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsGenerator(prop, 1, 1, 1, 1, 1, 2, false);
        assertEquals("  *g() {\n  }\n", prop.toSource(0));
    }

    @Test
    public void newLinesAreAllowedInGeneratorDefinition() {
        AstRoot root = parse("class X {\n*\ng\n(\n)\n{\n}\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsGenerator(prop, 1, 1, 1, 1, 2, 1, false);
        assertEquals("  *g() {\n  }\n", prop.toSource(0));
    }

    @Test
    public void generatorsCanBeStatic() {
        AstRoot root = parse("class X {\nstatic *g() {} }");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsGenerator(prop, 1, 1, 1, 8, 1, 9, true);
    }

    @Test
    public void staticIsAValidVariableIdentifier() {
        assertDoesNotThrow(() -> parse("var static = 42;"));
    }

    @Test
    public void constructorIsAValidVariableIdentifier() {
        assertDoesNotThrow(() -> parse("var constructor = 42;"));
    }

    @Test
    public void extendsIsAValidVariableIdentifier() {
        assertThrowsParseError("var extends = 42;", "missing variable name");
    }

    @Test
    public void constIsAValidPropertyName() {
        AstRoot root = parse("class Rectangle {\nconst\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsStandardProperty(prop, "const", 1, 1);
    }

    @Test
    public void letIsAValidPropertyName() {
        AstRoot root = parse("class Rectangle {\nlet\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsStandardProperty(prop, "let", 1, 1);
    }

    @Test
    public void staticIsAValidPropertyName() {
        AstRoot root = parse("class Rectangle {\nstatic = 1\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsStandardProperty(prop, "static", 1, 1);
        assertFalse(prop.isStatic());
    }

    @Test
    public void extendsIsAValidPropertyName() {
        AstRoot root = parse("class Rectangle {\nextends\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsStandardProperty(prop, "extends", 1, 1);
    }

    @Test
    public void staticIsAValidPropertyNameAndCanBeStatic() {
        AstRoot root = parse("class Rectangle {\nstatic static;\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        ClassProperty prop = getOnlyProp(classDefNode);
        assertLineColumnAre(prop, 1, 1);
        assertName(prop.getKey(), "static", 1, 8);
        assertTrue(prop.isStatic());
        assertNull(prop.getValue());
        assertEquals("  static static", prop.toSource(0));
    }

    @Test
    public void staticIsAValidPropertyNameAndCanBeAGetter() {
        AstRoot root = parse("class Rectangle {\nget static() {}\n}");

        ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
        ClassProperty prop = getOnlyProp(classDefNode);
        assertIsGetter(prop, "static", 1, 1, 1, 5, false);
    }

    private ClassProperty getOnlyProp(ClassDefNode classDefNode) {
        List<ClassProperty> properties = classDefNode.getProperties();
        assertEquals(1, properties.size());
        return properties.get(0);
    }

    private void assertIsStandardProperty(ClassProperty prop, String name, int line, int column) {
        assertLineColumnAre(prop, line, column);
        assertNull(prop.getJsDoc());
        assertName(prop.getKey(), name, line, column);
        assertFalse(prop.isStatic());
        assertFalse(prop.isGetterMethod());
        assertFalse(prop.isSetterMethod());
        assertFalse(prop.isNormalMethod());
        assertFalse(prop.isMethod());
    }

    private Name assertName(AstNode node, String identifier, int line, int column) {
        Name name = assertInstanceOf(Name.class, node);
        assertEquals(identifier, name.getIdentifier());
        assertLineColumnAre(name, line, column);
        return name;
    }

    private void assertIsMethod(ClassProperty prop, String name, int line, int column) {
        assertLineColumnAre(prop, line, column);
        assertName(prop.getKey(), name, line, column);
        assertTrue(prop.isMethod());
        assertFalse(prop.isGetterMethod());
        assertFalse(prop.isSetterMethod());
        assertTrue(prop.isNormalMethod());
        assertFalse(prop.isStatic());

        assertIsMethodNoArgs(prop.getValue(), line, column);
    }

    private void assertIsStaticMethod(
            ClassProperty prop,
            String name,
            int propLine,
            int propColumn,
            int fnLine,
            int fnColumn) {
        assertLineColumnAre(prop, propLine, propColumn);
        assertName(prop.getKey(), name, fnLine, fnColumn);
        assertTrue(prop.isMethod());
        assertFalse(prop.isGetterMethod());
        assertFalse(prop.isSetterMethod());
        assertTrue(prop.isNormalMethod());
        assertTrue(prop.isStatic());

        assertIsMethodNoArgs(prop.getValue(), fnLine, fnColumn);
    }

    private FunctionNode assertIsMethodNoArgs(AstNode value, int line, int column) {
        FunctionNode fun = assertInstanceOf(FunctionNode.class, value);
        assertEquals("", fun.getName());
        assertLineColumnAre(fun, line, column);
        assertTrue(fun.getParams().isEmpty());
        assertTrue(fun.isMethod());
        assertEquals(FunctionNode.FUNCTION_EXPRESSION, fun.getFunctionType());
        assertInstanceOf(Block.class, fun.getBody());
        return fun;
    }

    private void assertIsGetter(
            ClassProperty getter,
            String name,
            int propLine,
            int propColumn,
            int fnLine,
            int fnColumn,
            boolean isStatic) {
        assertLineColumnAre(getter, propLine, propColumn);
        assertName(getter.getKey(), name, fnLine, fnColumn);
        assertTrue(getter.isMethod());
        assertTrue(getter.isGetterMethod());
        assertFalse(getter.isSetterMethod());
        assertFalse(getter.isNormalMethod());
        assertEquals(isStatic, getter.isStatic());
        assertIsMethodNoArgs(getter.getValue(), fnLine, fnColumn);
    }

    private void assertIsSetter(
            ClassProperty setter,
            String name,
            int propLine,
            int propColumn,
            int fnLine,
            int fnColumn,
            boolean isStatic) {
        assertLineColumnAre(setter, propLine, propColumn);
        assertName(setter.getKey(), name, fnLine, fnColumn);
        assertTrue(setter.isMethod());
        assertFalse(setter.isGetterMethod());
        assertTrue(setter.isSetterMethod());
        assertFalse(setter.isNormalMethod());
        assertEquals(isStatic, setter.isStatic());

        FunctionNode fun = assertInstanceOf(FunctionNode.class, setter.getValue());
        assertEquals("", fun.getName());
        assertLineColumnAre(fun, fnLine, fnColumn);
        assertEquals(1, fun.getParams().size());
        assertTrue(fun.isMethod());
        assertEquals(FunctionNode.FUNCTION_EXPRESSION, fun.getFunctionType());
        assertInstanceOf(Block.class, fun.getBody());
    }

    private void assertIsGenerator(
            ClassProperty prop,
            int propLine,
            int propColumn,
            int keyLine,
            int keyColumn,
            int nameLine,
            int nameColumn,
            boolean isStatic) {
        assertLineColumnAre(prop, propLine, propColumn);
        GeneratorMethodDefinition key =
                assertInstanceOf(GeneratorMethodDefinition.class, prop.getKey());
        assertLineColumnAre(key, keyLine, keyColumn);
        assertName(key.getMethodName(), "g", nameLine, nameColumn);
        assertTrue(prop.isMethod());
        assertFalse(prop.isGetterMethod());
        assertFalse(prop.isSetterMethod());
        assertTrue(prop.isNormalMethod());
        assertEquals(isStatic, prop.isStatic());

        FunctionNode fun = assertIsMethodNoArgs(prop.getValue(), keyLine, keyColumn);
        assertTrue(fun.isES6Generator());
    }

    @Test
    public void cannotBeParsedInPreEs6Mode() {
        environment.setLanguageVersion(Context.VERSION_1_8);
        assertThrowsParseError("class Rectangle {};", "missing ; before statement");
    }
}
