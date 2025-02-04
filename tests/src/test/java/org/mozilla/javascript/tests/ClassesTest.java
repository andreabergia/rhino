package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mozilla.javascript.tests.ParserLineColumnNumberTest.assertLineColumnAre;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.ClassDefNode;
import org.mozilla.javascript.ast.Name;

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

		@Test
		public void basicDeclaration() {
			AstRoot root = parse(
					"class Rectangle {}"
			);

			ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
			assertLineColumnAre(classDefNode, 0, 1);
			
			Name className = classDefNode.getClassName();
			assertLineColumnAre(className, 0, 7);
			assertEquals("Rectangle", className.getIdentifier());
		}

		@Test
		public void classInExpression() {
			AstRoot root = parse(
					"class {}"
			);

			ClassDefNode classDefNode = assertInstanceOf(ClassDefNode.class, root.getFirstChild());
			assertLineColumnAre(classDefNode, 0, 1);
			assertNull(classDefNode.getClassName());
		}
	}
}
