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
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;

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

        //  @Test
        //        public void classesCanHaveOnlyOneConstructor() {
        //            shouldThrowParseError(
        //                    "class Rectangle {\n" + "  constructor() {}\n" + "  constructor()
        // {}\n" + "}",
        //                    "Duplicate constructor definition");
        //        }

        @Test
        public void constructorShouldBeMethod() {
            shouldThrowParseError(
                    "class Rectangle {\n" + "  constructor: function() {}\n" + "}",
                    "missing ( before function parameters.");
        }

        @Test
        public void constructorShouldNotBeProperties() {
            shouldThrowParseError(
                    "class Rectangle {\n" + "  constructor = 42\n" + "}",
                    "Invalid constructor definition");
        }

        @Test
        public void constructorShouldNotBeGenerators() {
            shouldThrowParseError(
                    "class Rectangle {\n" + "  *constructor() {}\n" + "}",
                    "Invalid constructor definition");
        }

        @Test
        public void propertiesCanBeDefined() {
            AstRoot root = parse("class Rectangle {\n" + "  x;\n" + "}");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

            List<ClassProperty> properties = classDefNode.getProperties();
            assertEquals(1, properties.size());
            ClassProperty prop = properties.get(0);
            assertNull(prop.getJsDoc());
            Name name = assertInstanceOf(Name.class, prop.getName());
            assertNull(prop.getJsDoc());
            assertEquals("x", name.getIdentifier());
            assertLineColumnAre(name, 1, 3);
            assertNull(prop.getValue());
            assertFalse(prop.isStatic());
            assertTrue(prop.isShorthand());
            assertFalse(prop.isGetterMethod());
            assertFalse(prop.isSetterMethod());
            assertFalse(prop.isNormalMethod());
            assertFalse(prop.isMethod());
        }

        @Test
        public void lastSemicolonInPropertiesIsOptional() {
            AstRoot root = parse("class Rectangle {\n" + "  x\n" + "}");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

            List<ClassProperty> properties = classDefNode.getProperties();
            assertEquals(1, properties.size());
            ClassProperty prop = properties.get(0);
            Name name = assertInstanceOf(Name.class, prop.getName());
            assertEquals("x", name.getIdentifier());
            assertLineColumnAre(name, 1, 3);
        }

        @Test
        public void propertiesCanHaveJsDoc() {
            AstRoot root = parse("class Rectangle {\n" + "  /** documentation */ x;\n" + "}");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

            List<ClassProperty> properties = classDefNode.getProperties();
            assertEquals(1, properties.size());
            ClassProperty prop = properties.get(0);
            assertNull(prop.getJsDoc());
            Name name = assertInstanceOf(Name.class, prop.getName());
            assertEquals("/** documentation */", name.getJsDoc());
        }

        @Test
        public void propertiesCanBeInitialized() {
            AstRoot root = parse("class Rectangle {\n" + "  x = 0;\n" + "}");

            ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
            assertEquals("Rectangle", classDefNode.getClassName().getIdentifier());

            List<ClassProperty> properties = classDefNode.getProperties();
            assertEquals(1, properties.size());
            ClassProperty prop = properties.get(0);
            Name name = assertInstanceOf(Name.class, prop.getName());
            assertEquals("x", name.getIdentifier());
            assertLineColumnAre(name, 1, 3);
            NumberLiteral expression = assertInstanceOf(NumberLiteral.class, prop.getValue());
            assertLineColumnAre(expression, 1, 7);
            assertEquals(0, expression.getNumber());
        }

        // TODO:
        //  - static properties
        //  - duplicate properties
        //  - properties with initializer (x = 1),
        //  - properties with getter / setter
        //  - no duplicate name in getter / setter
        //  - methods
        //  - static methods
        //  - extends and super call
    }
}
