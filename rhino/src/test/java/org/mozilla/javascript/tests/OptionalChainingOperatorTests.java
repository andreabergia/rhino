package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class OptionalChainingOperatorTests {
	@Test
	public void testOptionalChainingOperatorRequiresEs6() {
		// TODO
//		Utils.assertWithAllOptimizationLevelsES6("val", "var a = {b: 'val'}; a?.b");
	}

	@Test
    public void testOptionalChainingOperatorSimpleNames() {
		Utils.assertWithAllOptimizationLevelsES6("val", "var a = {b: 'val'}; a?.b");
		Utils.assertWithAllOptimizationLevelsES6("val", "var a = {b: {c: 'val'}}; a?.b?.c");
		Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "var a = null; a?.b");
		Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "var a = undefined; a?.b");
	}

	@Test
	public void testOptionalChainingOperatorSpecialRef() {
		Utils.assertWithAllOptimizationLevelsES6(true, "var a = {}; a?.__proto__ === Object.prototype");
		Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "var a = null; a?.__proto__");
		Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "var a = undefined; a?.__proto__");
	}

	@Test
	public void testOptionalChainingOperatorAfterExpression() {
		Utils.assertWithAllOptimizationLevelsES6(1, "var a = {b: 'x'}; a.b?.length");
		Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "var a = {b: 'x'}; a.c?.length");
		Utils.assertWithAllOptimizationLevelsES6(Undefined.instance, "var a = [1, 2, 3]; a[42]?.name");
	}

	@Test
	public void testOptionalChainingOperatorEvaluatesLeftHandSideOnlyOnce() {
		Utils.assertWithAllOptimizationLevelsES6(1, 
				"var counter = 0;\n" +
						"function f() {\n" +
						"  ++counter;\n" +
						"  return 'abc';\n" +
						"}\n" +
						"f()?.length;\n" +
						"counter\n");
	}

//        Utils.runWithAllOptimizationLevels(
//                cx -> {
//                    String sourceName = "optionalChainingOperator";
//                    Scriptable scope = cx.initStandardObjects();
//
//	                String script0 = "var a = {b: 'val'}; a?.b";
//	                assertEquals(
//				"val",
//			                cx.evaluateString(scope, script0, sourceName, 1, null));
//					
//                    String script = " var a = {name: 'val'}; a.outerProp?.innerProp";
//                    assertEquals(
//                            Undefined.instance,
//                            cx.evaluateString(scope, script, sourceName, 1, null));
//
//                    String script2 =
//                            " var a = {outerProp: {innerProp: 'val' } }; a.outerProp?.innerProp";
//                    assertEquals("val", cx.evaluateString(scope, script2, sourceName, 1, null));
//
//                    String script3 =
//                            " var a = {outerProp: {innerProp: { innerInnerProp: {name: 'val' } } } }; a.outerProp?.innerProp?.missingProp?.name";
//                    assertEquals(
//                            Undefined.instance,
//                            cx.evaluateString(scope, script3, sourceName, 1, null));
//
//                    String script4 =
//                            " var a = {outerProp: {innerProp: { innerInnerProp: {name: 'val' } } } }; a.outerProp?.innerProp?.innerInnerProp?.name";
//                    assertEquals("val", cx.evaluateString(scope, script4, sourceName, 1, null));
//
//                    // NOT WORKING YET
//                    //                    String script5 = " var a = {};
//                    // a.someNonExistentMethod?.()";
//                    //                    assertEquals(
//                    //                            Undefined.instance,
//                    //                            cx.evaluateString(scope, script5, sourceName, 1,
//                    // null));
//
//                    return null;
//                });
}
