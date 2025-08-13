package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.testutils.Utils;

class ClassesTest {
    //	@Test
    //	void fn() {
    //		String script =
    //				"function Foo (){} "
    //						+ "var c = new Foo;\n"
    //						+ "typeof c === 'object' && c instanceof Foo";
    //
    //		try (Context cx = Context.enter()) {
    //			cx.setLanguageVersion(Context.VERSION_ES6);
    //			cx.setInterpretedMode(true); // TODO: eventually test also in compiled mode
    //
    //			Scriptable scope = cx.initStandardObjects(new TopLevel());
    //
    //			Object res = cx.evaluateString(scope, script, "test", 1, null);
    //			assertEquals(true, res);
    //		}
    //	}
    //

    @Test
    void classAsStatementTypeofInstanceof() {
        String script =
                "class Foo { constructor(){} }"
                        + "var c = new Foo;\n"
                        + "typeof c === 'object' && c instanceof Foo";
        Object res = Utils.executeScript(script, true); // TODO: multiple modes
        assertEquals(true, res);
    }

	@Test
	void cannotCallClassConstructorWithoutNew() {
		String script =
				"class Foo { constructor(){} }"
						+ "Foo()";
		EcmaError err = assertThrows(EcmaError.class, () -> Utils.executeScript(script, true));// TODO: multiple modes
assertEquals("TypeError: Class constructor Foo cannot be invoked without new (myScript.js#1)", err.getMessage());
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
}
