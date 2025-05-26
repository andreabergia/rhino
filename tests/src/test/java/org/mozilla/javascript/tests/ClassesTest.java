package org.mozilla.javascript.tests;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.ClassDefNode;
import org.mozilla.javascript.ast.ClassProperty;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.ComputedPropertyKey;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.StringLiteral;

class ClassesTest {
    @Nested
    class Parsing {
        private CompilerEnvirons environment;

        @BeforeEach
        public void setUp() throws Exception {
            environment = new CompilerEnvirons();
            environment.setLanguageVersion(Context.VERSION_ES6);
        }

        private AstRoot parse(String source) {
            return ParserTest.parse(source, null, null, true, environment);
        }

        private void shouldThrowParseError(String source, String expectedError) {
            ParserTest.parse(source, new String[] {expectedError}, null, true, environment);
        }

        @Test
        public void basicDeclaration() {
            AstRoot root = parse("class Rectangle {}");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertLineColumnAre(classDefNode, 0, 1);

            Name className = classDefNode.getClassName();
            assertLineColumnAre(className, 0, 7);
            assertEquals("Rectangle", className.getIdentifier());
            assertNull(classDefNode.getConstructor());
        }

        @Test
        public void classInExpression() {
            AstRoot root = parse("class {}");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertNull(classDefNode.getClassName());
            assertNull(classDefNode.getConstructor());
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
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

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

//        @Test
//        public void classesCanHaveOnlyOneConstructor() {
//            shouldThrowParseError(
//                    "class Rectangle {\n"
//                            + "  constructor() {}\n"
//                            + "  constructor() {}\n"
//                            + "}",
//                    "duplicate constructor definition");
//        }

        @Test
        public void constructorMustBeMethod() {
            shouldThrowParseError(
                    "class Rectangle { constructor: function() {} }",
                    "missing ( before function parameters.");
        }

        @Test
        public void constructorShouldNotBeProperties() {
            shouldThrowParseError(
                    "class Rectangle { constructor = 42 }", "invalid constructor definition");
        }

        @Test
        public void constructorShouldNotBeGenerators() {
            shouldThrowParseError(
                    "class Rectangle { *constructor() {} }", "invalid constructor definition");
        }

        @Test
        public void constructorShouldNotBeStatic() {
            shouldThrowParseError(
                    "class Rectangle { static constructor() {} }", "constructor cannot be static");
        }

        @Test
        public void constructorShouldNotBeGetter() {
            shouldThrowParseError(
                    "class Rectangle { get constructor() {} }", "invalid property id");
        }

        @Test
        public void propertiesCanBeDefined() {
            AstRoot root = parse("class Rectangle {\n  x;\n}");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

            ClassProperty prop = getOnlyProp(classDefNode);
            assertLineColumnAre(prop, 1, 3);
            assertNull(prop.getJsDoc());
            Name name = assertInstanceOf(Name.class, prop.getKey());
            assertNull(prop.getJsDoc());
            assertEquals("x", name.getIdentifier());
            assertLineColumnAre(name, 1, 3);
            assertNull(prop.getValue());
            assertFalse(prop.isStatic());
            assertFalse(prop.isShorthand());
            assertFalse(prop.isGetterMethod());
            assertFalse(prop.isSetterMethod());
            assertFalse(prop.isNormalMethod());
            assertFalse(prop.isMethod());
        }

        @Test
        public void propertiesCanBeStatic() {
            AstRoot root = parse("class Rectangle { static x; }");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

            ClassProperty prop = getOnlyProp(classDefNode);
            assertLineColumnAre(prop, 0, 19);
            Name name = assertInstanceOf(Name.class, prop.getKey());
            assertEquals("x", name.getIdentifier());
            assertTrue(prop.isStatic());
            assertLineColumnAre(name, 0, 26);
        }

        @Test
        public void propertiesCanHaveCommentsBetweenStaticAndPropName() {
            AstRoot root = parse("class Rectangle { static /* comment */ x; }");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

            ClassProperty prop = getOnlyProp(classDefNode);
            assertLineColumnAre(prop, 0, 19);
            Name name = assertInstanceOf(Name.class, prop.getKey());
            assertEquals("x", name.getIdentifier());
            assertTrue(prop.isStatic());
            assertLineColumnAre(name, 0, 40);
        }

        @Test
        public void lastSemicolonInPropertiesIsOptional() {
            AstRoot root = parse("class Rectangle {\n" + "  x\n" + "}");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

            ClassProperty prop = getOnlyProp(classDefNode);
            Name name = assertInstanceOf(Name.class, prop.getKey());
            assertEquals("x", name.getIdentifier());
            assertLineColumnAre(name, 1, 3);
        }

        @Test
        public void propertiesCanHaveJsDoc() {
            AstRoot root = parse("class Rectangle {\n" + "  /** documentation */ x;\n" + "}");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

            ClassProperty prop = getOnlyProp(classDefNode);
            assertLineColumnAre(prop, 1, 24);
            assertNull(prop.getJsDoc());
            Name name = assertInstanceOf(Name.class, prop.getKey());
            assertEquals("/** documentation */", name.getJsDoc());
        }

        @Test
        public void propertiesCanBeInitialized() {
            AstRoot root = parse("class Rectangle {\n  x = 0;\n}");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

            ClassProperty prop = getOnlyProp(classDefNode);
            assertFalse(prop.isStatic());
            assertLineColumnAre(prop, 1, 3);
        }

        @Test
        public void propertiesCanBeStaticAndInitialized() {
            AstRoot root = parse("class Rectangle {\n  static x = 0;\n}");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

            ClassProperty prop = getOnlyProp(classDefNode);
            assertTrue(prop.isStatic());
            assertLineColumnAre(prop, 1, 3);
        }

        @Test
        public void propertyNameCanBeLiteralNumber() {
            AstRoot root = parse("class X { 42 }");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("X", classDefNode.getClassName().getIdentifier());

            ClassProperty prop = getOnlyProp(classDefNode);
            NumberLiteral literal = assertInstanceOf(NumberLiteral.class, prop.getKey());
            assertEquals("42", literal.getValue());
            assertLineColumnAre(literal, 0, 11);
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
        }

        @Test
        public void propertyNameCanBeExpressions() {
            AstRoot root = parse("class X { [1 + 2] }");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("X", classDefNode.getClassName().getIdentifier());

            ClassProperty prop = getOnlyProp(classDefNode);
            ComputedPropertyKey key = assertInstanceOf(ComputedPropertyKey.class, prop.getKey());
            assertEquals("[1 + 2]", key.toSource());
            assertLineColumnAre(key, 0, 11);
            assertInstanceOf(InfixExpression.class, key.getExpression());
        }

        @Test
        public void propertyNamesCanBeDuplicated() {
            AstRoot root = parse("class X {\n  x;\n  x = 1; }");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("X", classDefNode.getClassName().getIdentifier());

            List<ClassProperty> properties = classDefNode.getProperties();
            assertEquals(2, properties.size());

            ClassProperty prop = properties.get(0);
            Name key = assertInstanceOf(Name.class, prop.getKey());
            assertEquals("x", key.getIdentifier());
            assertLineColumnAre(key, 1, 3);
            assertNull( prop.getValue());

             prop = properties.get(1);
             key = assertInstanceOf(Name.class, prop.getKey());
            assertEquals("x", key.getIdentifier());
            assertLineColumnAre(key, 2, 3);
            NumberLiteral initializer = assertInstanceOf(NumberLiteral.class, prop.getValue());
            assertEquals("1", initializer.getValue());
        }

        private ClassProperty getOnlyProp(ClassDefNode classDefNode) {
            List<ClassProperty> properties = classDefNode.getProperties();
            assertEquals(1, properties.size());
	        return properties.get(0);
        }

        // TODO:
        //  - methods
        //  - static methods
        //  - properties with getter / setter
        //  - extends
        //  - super call from constructor
    }
}
