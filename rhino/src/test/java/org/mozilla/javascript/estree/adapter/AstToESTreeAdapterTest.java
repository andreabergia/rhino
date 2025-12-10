package org.mozilla.javascript.estree.adapter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.estree.ESTree;
import org.mozilla.javascript.estree.nodes.Program;
import org.mozilla.javascript.estree.nodes.base.*;
import org.mozilla.javascript.estree.nodes.declarations.*;
import org.mozilla.javascript.estree.nodes.expressions.*;
import org.mozilla.javascript.estree.nodes.literals.*;
import org.mozilla.javascript.estree.nodes.statements.*;

/*
 * Tests the complete conversion pipeline from JavaScript source → Rhino AST → ESTree.
 */
class AstToESTreeAdapterTest {

    private Program parseAndAdapt(String code) {
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecordingComments(true);
        env.setRecordingLocalJsDocComments(true);

        Parser parser = new Parser(env);
        AstRoot ast = parser.parse(code, "test.js", 1);

        return ESTree.from(ast, code, "test.js");
    }

    // ==================== Simple Statements ====================

    @Test
    void testEmptyProgram() {
        Program program = parseAndAdapt("");
        assertEquals("Program", program.type());
        assertEquals(0, program.body().size());
        assertEquals("script", program.sourceType());
    }

    @Test
    void testVariableDeclaration() {
        Program program = parseAndAdapt("var x = 1;");

        assertEquals(1, program.body().size());
        Statement stmt = program.body().get(0);

        assertTrue(stmt instanceof VariableDeclaration);
        VariableDeclaration varDecl = (VariableDeclaration) stmt;

        assertEquals("VariableDeclaration", varDecl.type());
        assertEquals(VariableDeclarationKind.VAR, varDecl.kind());
        assertEquals(1, varDecl.declarations().size());

        VariableDeclarator declarator = varDecl.declarations().get(0);
        assertEquals("VariableDeclarator", declarator.type());

        assertTrue(declarator.id() instanceof Identifier);
        Identifier id = (Identifier) declarator.id();
        assertEquals("x", id.name());

        assertTrue(declarator.init() instanceof SimpleLiteral);
        SimpleLiteral init = (SimpleLiteral) declarator.init();
        assertEquals(1.0, init.value());
    }

    @Test
    void testMultipleVariableDeclarations() {
        Program program = parseAndAdapt("var x = 1, y = 2;");

        VariableDeclaration varDecl = (VariableDeclaration) program.body().get(0);
        assertEquals(2, varDecl.declarations().size());

        VariableDeclarator first = varDecl.declarations().get(0);
        assertEquals("x", ((Identifier) first.id()).name());
        assertEquals(1.0, ((SimpleLiteral) first.init()).value());

        VariableDeclarator second = varDecl.declarations().get(1);
        assertEquals("y", ((Identifier) second.id()).name());
        assertEquals(2.0, ((SimpleLiteral) second.init()).value());
    }

    @Test
    void testLetAndConst() {
        Program letProgram = parseAndAdapt("let x = 1;");
        VariableDeclaration letDecl = (VariableDeclaration) letProgram.body().get(0);
        assertEquals(VariableDeclarationKind.LET, letDecl.kind());

        Program constProgram = parseAndAdapt("const y = 2;");
        VariableDeclaration constDecl = (VariableDeclaration) constProgram.body().get(0);
        assertEquals(VariableDeclarationKind.CONST, constDecl.kind());
    }

    @Test
    void testExpressionStatement() {
        Program program = parseAndAdapt("x + 1;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);

        assertEquals("ExpressionStatement", exprStmt.type());
        assertTrue(exprStmt.expression() instanceof BinaryExpression);
    }

    @Test
    void testIfStatement() {
        Program program = parseAndAdapt("if (x > 0) { return 1; }");

        org.mozilla.javascript.estree.nodes.statements.IfStatement ifStmt =
                (org.mozilla.javascript.estree.nodes.statements.IfStatement) program.body().get(0);

        assertEquals("IfStatement", ifStmt.type());
        assertTrue(ifStmt.test() instanceof BinaryExpression);
        assertTrue(ifStmt.consequent() instanceof BlockStatement);
        assertNull(ifStmt.alternate());
    }

    @Test
    void testIfElseStatement() {
        Program program = parseAndAdapt("if (x > 0) { return 1; } else { return -1; }");

        org.mozilla.javascript.estree.nodes.statements.IfStatement ifStmt =
                (org.mozilla.javascript.estree.nodes.statements.IfStatement) program.body().get(0);

        assertNotNull(ifStmt.alternate());
        assertTrue(ifStmt.alternate() instanceof BlockStatement);
    }

    @Test
    void testWhileLoop() {
        Program program = parseAndAdapt("while (x < 10) { x++; }");

        org.mozilla.javascript.estree.nodes.statements.WhileStatement whileStmt =
                (org.mozilla.javascript.estree.nodes.statements.WhileStatement)
                        program.body().get(0);

        assertEquals("WhileStatement", whileStmt.type());
        assertTrue(whileStmt.test() instanceof BinaryExpression);
        assertTrue(whileStmt.body() instanceof BlockStatement);
    }

    @Test
    void testDoWhileLoop() {
        Program program = parseAndAdapt("do { x++; } while (x < 10);");

        DoWhileStatement doWhileStmt = (DoWhileStatement) program.body().get(0);

        assertEquals("DoWhileStatement", doWhileStmt.type());
        assertTrue(doWhileStmt.body() instanceof BlockStatement);
        assertTrue(doWhileStmt.test() instanceof BinaryExpression);
    }

    @Test
    void testForLoop() {
        Program program = parseAndAdapt("for (var i = 0; i < 10; i++) { sum += i; }");

        org.mozilla.javascript.estree.nodes.statements.ForStatement forStmt =
                (org.mozilla.javascript.estree.nodes.statements.ForStatement) program.body().get(0);

        assertEquals("ForStatement", forStmt.type());
        assertTrue(forStmt.init() instanceof VariableDeclaration);
        assertTrue(forStmt.test() instanceof BinaryExpression);
        assertTrue(forStmt.update() instanceof UpdateExpression);
        assertTrue(forStmt.body() instanceof BlockStatement);
    }

    @Test
    void testForLoopWithoutInit() {
        Program program = parseAndAdapt("for (; i < 10; i++) {}");

        org.mozilla.javascript.estree.nodes.statements.ForStatement forStmt =
                (org.mozilla.javascript.estree.nodes.statements.ForStatement) program.body().get(0);

        assertNull(forStmt.init());
        assertNotNull(forStmt.test());
        assertNotNull(forStmt.update());
    }

    // ==================== Control Flow ====================

    @Test
    void testReturnStatement() {
        Program program = parseAndAdapt("function f() { return 42; }");

        FunctionDeclaration func = (FunctionDeclaration) program.body().get(0);
        org.mozilla.javascript.estree.nodes.statements.ReturnStatement returnStmt =
                (org.mozilla.javascript.estree.nodes.statements.ReturnStatement)
                        func.body().body().get(0);

        assertEquals("ReturnStatement", returnStmt.type());
        assertTrue(returnStmt.argument() instanceof SimpleLiteral);
        assertEquals(42.0, ((SimpleLiteral) returnStmt.argument()).value());
    }

    @Test
    void testReturnWithoutValue() {
        Program program = parseAndAdapt("function f() { return; }");

        FunctionDeclaration func = (FunctionDeclaration) program.body().get(0);
        org.mozilla.javascript.estree.nodes.statements.ReturnStatement returnStmt =
                (org.mozilla.javascript.estree.nodes.statements.ReturnStatement)
                        func.body().body().get(0);

        assertNull(returnStmt.argument());
    }

    @Test
    void testBreakStatement() {
        Program program = parseAndAdapt("while (true) { break; }");

        org.mozilla.javascript.estree.nodes.statements.WhileStatement whileStmt =
                (org.mozilla.javascript.estree.nodes.statements.WhileStatement)
                        program.body().get(0);
        BlockStatement body = (BlockStatement) whileStmt.body();
        org.mozilla.javascript.estree.nodes.statements.BreakStatement breakStmt =
                (org.mozilla.javascript.estree.nodes.statements.BreakStatement) body.body().get(0);

        assertEquals("BreakStatement", breakStmt.type());
        assertNull(breakStmt.label());
    }

    @Test
    void testContinueStatement() {
        Program program = parseAndAdapt("while (true) { continue; }");

        org.mozilla.javascript.estree.nodes.statements.WhileStatement whileStmt =
                (org.mozilla.javascript.estree.nodes.statements.WhileStatement)
                        program.body().get(0);
        BlockStatement body = (BlockStatement) whileStmt.body();
        org.mozilla.javascript.estree.nodes.statements.ContinueStatement continueStmt =
                (org.mozilla.javascript.estree.nodes.statements.ContinueStatement)
                        body.body().get(0);

        assertEquals("ContinueStatement", continueStmt.type());
        assertNull(continueStmt.label());
    }

    @Test
    void testThrowStatement() {
        Program program = parseAndAdapt("throw new Error('test');");

        org.mozilla.javascript.estree.nodes.statements.ThrowStatement throwStmt =
                (org.mozilla.javascript.estree.nodes.statements.ThrowStatement)
                        program.body().get(0);

        assertEquals("ThrowStatement", throwStmt.type());
        assertTrue(throwStmt.argument() instanceof NewExpression);
    }

    @Test
    void testTryCatchFinally() {
        Program program =
                parseAndAdapt("try { risky(); } catch (e) { handle(e); } finally { cleanup(); }");

        TryStatement tryStmt = (TryStatement) program.body().get(0);

        assertEquals("TryStatement", tryStmt.type());
        assertTrue(tryStmt.block() instanceof BlockStatement);
        assertNotNull(tryStmt.handler());
        assertNotNull(tryStmt.finalizer());

        org.mozilla.javascript.estree.nodes.clauses.CatchClause catchClause = tryStmt.handler();
        assertEquals("CatchClause", catchClause.type());
        assertTrue(catchClause.param() instanceof Identifier);
        assertEquals("e", ((Identifier) catchClause.param()).name());
    }

    // ==================== Expressions ====================

    @Test
    void testBinaryExpression() {
        Program program = parseAndAdapt("x + y;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        BinaryExpression binary = (BinaryExpression) exprStmt.expression();

        assertEquals("BinaryExpression", binary.type());
        assertEquals("+", binary.operator());
        assertEquals("x", ((Identifier) binary.left()).name());
        assertEquals("y", ((Identifier) binary.right()).name());
    }

    @Test
    void testLogicalExpression() {
        Program program = parseAndAdapt("x && y;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        LogicalExpression logical = (LogicalExpression) exprStmt.expression();

        assertEquals("LogicalExpression", logical.type());
        assertEquals("&&", logical.operator());
    }

    @Test
    void testUnaryExpression() {
        Program program = parseAndAdapt("!x;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        org.mozilla.javascript.estree.nodes.expressions.UnaryExpression unary =
                (org.mozilla.javascript.estree.nodes.expressions.UnaryExpression)
                        exprStmt.expression();

        assertEquals("UnaryExpression", unary.type());
        assertEquals("!", unary.operator());
        assertTrue(unary.prefix());
    }

    @Test
    void testUpdateExpression() {
        Program program = parseAndAdapt("x++;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        org.mozilla.javascript.estree.nodes.expressions.UpdateExpression update =
                (org.mozilla.javascript.estree.nodes.expressions.UpdateExpression)
                        exprStmt.expression();

        assertEquals("UpdateExpression", update.type());
        assertEquals("++", update.operator());
        assertFalse(update.prefix());
    }

    @Test
    void testAssignmentExpression() {
        Program program = parseAndAdapt("x = 5;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        AssignmentExpression assignment = (AssignmentExpression) exprStmt.expression();

        assertEquals("AssignmentExpression", assignment.type());
        assertEquals("=", assignment.operator());
        assertTrue(assignment.left() instanceof Identifier);
        assertTrue(assignment.right() instanceof SimpleLiteral);
    }

    @Test
    void testCompoundAssignment() {
        Program program = parseAndAdapt("x += 5;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        AssignmentExpression assignment = (AssignmentExpression) exprStmt.expression();

        assertEquals("+=", assignment.operator());
    }

    @Test
    void testConditionalExpression() {
        Program program = parseAndAdapt("x ? 1 : 2;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        ConditionalExpression conditional = (ConditionalExpression) exprStmt.expression();

        assertEquals("ConditionalExpression", conditional.type());
        assertTrue(conditional.test() instanceof Identifier);
        assertTrue(conditional.consequent() instanceof SimpleLiteral);
        assertTrue(conditional.alternate() instanceof SimpleLiteral);
    }

    @Test
    void testSequenceExpression() {
        Program program = parseAndAdapt("x++, y++, z++;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        SequenceExpression sequence = (SequenceExpression) exprStmt.expression();

        assertEquals("SequenceExpression", sequence.type());
        assertEquals(3, sequence.expressions().size());
    }

    @Test
    void testCallExpression() {
        Program program = parseAndAdapt("foo(1, 2);");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        CallExpression call = (CallExpression) exprStmt.expression();

        assertEquals("CallExpression", call.type());
        assertEquals("foo", ((Identifier) call.callee()).name());
        assertEquals(2, call.arguments().size());
    }

    @Test
    void testNewExpression() {
        Program program = parseAndAdapt("new Foo(1, 2);");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        NewExpression newExpr = (NewExpression) exprStmt.expression();

        assertEquals("NewExpression", newExpr.type());
        assertEquals("Foo", ((Identifier) newExpr.callee()).name());
        assertEquals(2, newExpr.arguments().size());
    }

    @Test
    void testMemberExpression() {
        Program program = parseAndAdapt("obj.prop;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        MemberExpression member = (MemberExpression) exprStmt.expression();

        assertEquals("MemberExpression", member.type());
        assertEquals("obj", ((Identifier) member.object()).name());
        assertEquals("prop", ((Identifier) member.property()).name());
        assertFalse(member.computed());
    }

    @Test
    void testComputedMemberExpression() {
        Program program = parseAndAdapt("obj[key];");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        MemberExpression member = (MemberExpression) exprStmt.expression();

        assertEquals("obj", ((Identifier) member.object()).name());
        assertEquals("key", ((Identifier) member.property()).name());
        assertTrue(member.computed());
    }

    @Test
    void testThisExpression() {
        Program program = parseAndAdapt("this;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        ThisExpression thisExpr = (ThisExpression) exprStmt.expression();

        assertEquals("ThisExpression", thisExpr.type());
    }

    // ==================== Literals ====================

    @Test
    void testNumberLiteral() {
        Program program = parseAndAdapt("42;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        SimpleLiteral literal = (SimpleLiteral) exprStmt.expression();

        assertEquals("Literal", literal.type());
        assertEquals(42.0, literal.value());
        assertEquals("42", literal.raw());
    }

    @Test
    void testStringLiteral() {
        Program program = parseAndAdapt("'hello';");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        SimpleLiteral literal = (SimpleLiteral) exprStmt.expression();

        assertEquals("Literal", literal.type());
        assertEquals("hello", literal.value());
    }

    @Test
    void testBooleanLiterals() {
        Program trueProgram = parseAndAdapt("true;");
        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement trueStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        trueProgram.body().get(0);
        SimpleLiteral trueLiteral = (SimpleLiteral) trueStmt.expression();
        assertEquals(true, trueLiteral.value());

        Program falseProgram = parseAndAdapt("false;");
        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement falseStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        falseProgram.body().get(0);
        SimpleLiteral falseLiteral = (SimpleLiteral) falseStmt.expression();
        assertEquals(false, falseLiteral.value());
    }

    @Test
    void testNullLiteral() {
        Program program = parseAndAdapt("null;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        SimpleLiteral literal = (SimpleLiteral) exprStmt.expression();

        assertEquals("Literal", literal.type());
        assertNull(literal.value());
        assertEquals("null", literal.raw());
    }

    @Test
    void testRegExpLiteral() {
        Program program = parseAndAdapt("/abc/gi;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        RegExpLiteral regexp = (RegExpLiteral) exprStmt.expression();

        assertEquals("Literal", regexp.type());
        assertEquals("abc", regexp.regex().pattern());
        assertEquals("gi", regexp.regex().flags());
    }

    @Test
    void testArrayLiteral() {
        Program program = parseAndAdapt("[1, 2, 3];");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        ArrayExpression array = (ArrayExpression) exprStmt.expression();

        assertEquals("ArrayExpression", array.type());
        assertEquals(3, array.elements().size());
    }

    @Test
    void testSparseArray() {
        Program program = parseAndAdapt("[1, , 3];");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        ArrayExpression array = (ArrayExpression) exprStmt.expression();

        assertEquals(3, array.elements().size());
        assertNotNull(array.elements().get(0));
        assertNull(array.elements().get(1)); // Hole
        assertNotNull(array.elements().get(2));
    }

    @Test
    void testObjectLiteral() {
        Program program = parseAndAdapt("({x: 1, y: 2});");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);
        ObjectExpression object = (ObjectExpression) exprStmt.expression();

        assertEquals("ObjectExpression", object.type());
        assertEquals(2, object.properties().size());

        org.mozilla.javascript.estree.nodes.properties.Property prop1 = object.properties().get(0);
        assertEquals("init", prop1.kind());
        assertEquals("x", ((Identifier) prop1.key()).name());
        assertEquals(1.0, ((SimpleLiteral) prop1.value()).value());
    }

    // ==================== Functions ====================

    @Test
    void testFunctionDeclaration() {
        Program program = parseAndAdapt("function foo(a, b) { return a + b; }");

        FunctionDeclaration func = (FunctionDeclaration) program.body().get(0);

        assertEquals("FunctionDeclaration", func.type());
        assertEquals("foo", func.id().name());
        assertEquals(2, func.params().size());
        assertEquals("a", ((Identifier) func.params().get(0)).name());
        assertEquals("b", ((Identifier) func.params().get(1)).name());
        assertTrue(func.body() instanceof BlockStatement);
        assertFalse(func.generator());
        assertFalse(func.async());
    }

    @Test
    void testFunctionExpression() {
        Program program = parseAndAdapt("var f = function(x) { return x * 2; };");

        VariableDeclaration varDecl = (VariableDeclaration) program.body().get(0);
        org.mozilla.javascript.estree.nodes.expressions.FunctionExpression funcExpr =
                (org.mozilla.javascript.estree.nodes.expressions.FunctionExpression)
                        varDecl.declarations().get(0).init();

        assertEquals("FunctionExpression", funcExpr.type());
        assertNull(funcExpr.id()); // Anonymous
        assertEquals(1, funcExpr.params().size());
    }

    @Test
    void testNamedFunctionExpression() {
        Program program = parseAndAdapt("var f = function factorial(n) { return n; };");

        VariableDeclaration varDecl = (VariableDeclaration) program.body().get(0);
        org.mozilla.javascript.estree.nodes.expressions.FunctionExpression funcExpr =
                (org.mozilla.javascript.estree.nodes.expressions.FunctionExpression)
                        varDecl.declarations().get(0).init();

        assertNotNull(funcExpr.id());
        assertEquals("factorial", funcExpr.id().name());
    }

    // ==================== Position Tests ====================

    @Test
    void testPositionAccuracy() {
        String code = "var x = 1;\nvar y = 2;";
        Program program = parseAndAdapt(code);

        // First statement: "var x = 1;"
        Statement stmt1 = program.body().get(0);
        assertEquals(1, stmt1.loc().start().line());
        assertEquals(0, stmt1.loc().start().column());
        assertEquals(0, stmt1.start());
        assertEquals(10, stmt1.end());

        // Second statement: "var y = 2;"
        Statement stmt2 = program.body().get(1);
        assertEquals(2, stmt2.loc().start().line());
        assertEquals(0, stmt2.loc().start().column());
    }

    @Test
    void testRangeProperty() {
        Program program = parseAndAdapt("var x = 1;");
        Statement stmt = program.body().get(0);

        int[] range = stmt.range();
        assertEquals(2, range.length);
        assertEquals(stmt.start(), range[0]);
        assertEquals(stmt.end(), range[1]);
    }

    // ==================== Edge Cases ====================

    @Test
    void testEmptyBlock() {
        Program program = parseAndAdapt("{}");

        BlockStatement block = (BlockStatement) program.body().get(0);
        assertEquals(0, block.body().size());
    }

    @Test
    void testNestedBlocks() {
        Program program = parseAndAdapt("{ { { } } }");

        BlockStatement outer = (BlockStatement) program.body().get(0);
        BlockStatement middle = (BlockStatement) outer.body().get(0);
        BlockStatement inner = (BlockStatement) middle.body().get(0);

        assertEquals(0, inner.body().size());
    }

    @Test
    void testComplexExpression() {
        Program program = parseAndAdapt("(a + b) * (c - d) / e;");

        org.mozilla.javascript.estree.nodes.statements.ExpressionStatement exprStmt =
                (org.mozilla.javascript.estree.nodes.statements.ExpressionStatement)
                        program.body().get(0);

        // Should be a binary expression tree
        assertTrue(exprStmt.expression() instanceof BinaryExpression);
        BinaryExpression root = (BinaryExpression) exprStmt.expression();
        assertEquals("/", root.operator());
    }
}
