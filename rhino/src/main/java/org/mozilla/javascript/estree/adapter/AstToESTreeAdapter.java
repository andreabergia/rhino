package org.mozilla.javascript.estree.adapter;

import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.estree.nodes.Program;
import org.mozilla.javascript.estree.nodes.base.Expression;
import org.mozilla.javascript.estree.nodes.base.Identifier;
import org.mozilla.javascript.estree.nodes.base.Node;
import org.mozilla.javascript.estree.nodes.base.Pattern;
import org.mozilla.javascript.estree.nodes.base.Statement;
import org.mozilla.javascript.estree.nodes.clauses.CatchClause;
import org.mozilla.javascript.estree.nodes.clauses.SwitchCase;
import org.mozilla.javascript.estree.nodes.declarations.*;
import org.mozilla.javascript.estree.nodes.declarations.FunctionDeclaration;
import org.mozilla.javascript.estree.nodes.declarations.VariableDeclaration;
import org.mozilla.javascript.estree.nodes.declarations.VariableDeclarationKind;
import org.mozilla.javascript.estree.nodes.declarations.VariableDeclarator;
import org.mozilla.javascript.estree.nodes.expressions.*;
import org.mozilla.javascript.estree.nodes.expressions.ArrayExpression;
import org.mozilla.javascript.estree.nodes.expressions.AssignmentExpression;
import org.mozilla.javascript.estree.nodes.expressions.BinaryExpression;
import org.mozilla.javascript.estree.nodes.expressions.CallExpression;
import org.mozilla.javascript.estree.nodes.expressions.LogicalExpression;
import org.mozilla.javascript.estree.nodes.expressions.MemberExpression;
import org.mozilla.javascript.estree.nodes.expressions.ObjectExpression;
import org.mozilla.javascript.estree.nodes.expressions.SequenceExpression;
import org.mozilla.javascript.estree.nodes.expressions.ThisExpression;
import org.mozilla.javascript.estree.nodes.literals.BooleanLiteral;
import org.mozilla.javascript.estree.nodes.literals.NullLiteral;
import org.mozilla.javascript.estree.nodes.literals.NumberLiteral;
import org.mozilla.javascript.estree.nodes.literals.RegExpLiteral;
import org.mozilla.javascript.estree.nodes.literals.SimpleLiteral;
import org.mozilla.javascript.estree.nodes.literals.StringLiteral;
import org.mozilla.javascript.estree.nodes.properties.Property;
import org.mozilla.javascript.estree.nodes.statements.BlockStatement;
import org.mozilla.javascript.estree.nodes.statements.BreakStatement;
import org.mozilla.javascript.estree.nodes.statements.ContinueStatement;
import org.mozilla.javascript.estree.nodes.statements.DebuggerStatement;
import org.mozilla.javascript.estree.nodes.statements.DoWhileStatement;
import org.mozilla.javascript.estree.nodes.statements.EmptyStatement;
import org.mozilla.javascript.estree.nodes.statements.ExpressionStatement;
import org.mozilla.javascript.estree.nodes.statements.ForInStatement;
import org.mozilla.javascript.estree.nodes.statements.ForStatement;
import org.mozilla.javascript.estree.nodes.statements.IfStatement;
import org.mozilla.javascript.estree.nodes.statements.LabeledStatement;
import org.mozilla.javascript.estree.nodes.statements.ReturnStatement;
import org.mozilla.javascript.estree.nodes.statements.SwitchStatement;
import org.mozilla.javascript.estree.nodes.statements.ThrowStatement;
import org.mozilla.javascript.estree.nodes.statements.TryStatement;
import org.mozilla.javascript.estree.nodes.statements.WhileStatement;
import org.mozilla.javascript.estree.nodes.statements.WithStatement;
import org.mozilla.javascript.estree.types.AssignmentOperator;
import org.mozilla.javascript.estree.types.BinaryOperator;
import org.mozilla.javascript.estree.types.LogicalOperator;
import org.mozilla.javascript.estree.types.SourceLocation;
import org.mozilla.javascript.estree.types.UnaryOperator;
import org.mozilla.javascript.estree.types.UpdateOperator;

/**
 * Converts Rhino's AstNode tree to ESTree format.
 *
 * <p>This adapter traverses a Rhino AST and produces an immutable ESTree representation. Key
 * transformations:
 *
 * <ul>
 *   <li>Relative positions → absolute positions with line/column
 *   <li>Mutable nodes → immutable records
 *   <li>Token types → ESTree node types
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>
 * AstRoot rhinoAst = parser.parse(...);
 * AstToESTreeAdapter adapter = new AstToESTreeAdapter(sourceCode, "file.js");
 * Program estree = adapter.convertProgram(rhinoAst);
 * </pre>
 */
public class AstToESTreeAdapter {

    private final PositionConverter positionConverter;

    /**
     * Creates an adapter for converting Rhino AST to ESTree.
     *
     * @param sourceCode the source code text
     * @param sourceName optional filename or URI
     */
    public AstToESTreeAdapter(String sourceCode, String sourceName) {
        this.positionConverter = new PositionConverter(sourceCode, sourceName);
    }

    /**
     * Converts a Rhino AstRoot to an ESTree Program.
     *
     * @param root the Rhino AST root
     * @return the ESTree Program node
     */
    public Program convertProgram(AstRoot root) {
        List<Statement> body = new ArrayList<>();

        // Convert all top-level statements
        for (org.mozilla.javascript.Node child : root) {
            if (child instanceof AstNode astNode) {
                Node converted = convert(astNode);
                if (converted instanceof Statement stmt) {
                    body.add(stmt);
                } else {
                    throw new IllegalStateException(
                            "Program body must contain statements, found: "
                                    + converted.getClass().getSimpleName());
                }
            }
        }

        // Create position info for program
        int start = root.getAbsolutePosition();
        int end = start + root.getLength();
        SourceLocation loc = positionConverter.createLocation(start, end);

        return new Program(
                loc, start, end, List.of(), // leadingComments - TODO: Phase 4
                List.of(), // trailingComments
                List.of(), // innerComments
                "script", // sourceType - TODO: detect module vs script
                body);
    }

    /**
     * Converts any AstNode to its corresponding ESTree node.
     *
     * @param node the Rhino AST node
     * @return the ESTree node
     * @throws UnsupportedOperationException if the node type is not yet supported
     */
    public Node convert(AstNode node) {
        if (node == null) {
            return null;
        }

        int tokenType = node.getType();

        return switch (tokenType) {
            case Token.SCRIPT -> convertProgram((AstRoot) node);
            case Token.BLOCK -> convertBlock((org.mozilla.javascript.ast.Block) node);
            case Token.EXPR_VOID, Token.EXPR_RESULT ->
                    convertExpressionStatement(
                            (org.mozilla.javascript.ast.ExpressionStatement) node);
            case Token.IF -> convertIfStatement((org.mozilla.javascript.ast.IfStatement) node);
            case Token.RETURN ->
                    convertReturnStatement((org.mozilla.javascript.ast.ReturnStatement) node);
            case Token.VAR, Token.LET, Token.CONST ->
                    convertVariableDeclaration(
                            (org.mozilla.javascript.ast.VariableDeclaration) node);
            case Token.FUNCTION -> convertFunction(node);
            case Token.EMPTY ->
                    convertEmptyStatement((org.mozilla.javascript.ast.EmptyStatement) node);
            case Token.BREAK ->
                    convertBreakStatement((org.mozilla.javascript.ast.BreakStatement) node);
            case Token.CONTINUE ->
                    convertContinueStatement((org.mozilla.javascript.ast.ContinueStatement) node);
            case Token.WHILE -> convertWhileStatement((org.mozilla.javascript.ast.WhileLoop) node);
            case Token.DO -> convertDoWhileStatement((org.mozilla.javascript.ast.DoLoop) node);
            case Token.FOR -> convertForStatement((org.mozilla.javascript.ast.ForLoop) node);
            case Token.SWITCH ->
                    convertSwitchStatement((org.mozilla.javascript.ast.SwitchStatement) node);
            case Token.THROW ->
                    convertThrowStatement((org.mozilla.javascript.ast.ThrowStatement) node);
            case Token.TRY -> convertTryStatement((org.mozilla.javascript.ast.TryStatement) node);
            case Token.WITH ->
                    convertWithStatement((org.mozilla.javascript.ast.WithStatement) node);
            case Token.LABEL ->
                    convertLabeledStatement((org.mozilla.javascript.ast.LabeledStatement) node);
            case Token.DEBUGGER ->
                    convertDebuggerStatement((org.mozilla.javascript.ast.KeywordLiteral) node);

            // Expressions
            case Token.NAME -> convertIdentifier((Name) node);
            case Token.NUMBER ->
                    convertNumberLiteral((org.mozilla.javascript.ast.NumberLiteral) node);
            case Token.STRING ->
                    convertStringLiteral((org.mozilla.javascript.ast.StringLiteral) node);
            case Token.TRUE, Token.FALSE, Token.NULL ->
                    convertKeywordLiteral((org.mozilla.javascript.ast.KeywordLiteral) node);
            case Token.THIS ->
                    convertThisExpression((org.mozilla.javascript.ast.KeywordLiteral) node);
            case Token.ARRAYLIT ->
                    convertArrayExpression((org.mozilla.javascript.ast.ArrayLiteral) node);
            case Token.OBJECTLIT ->
                    convertObjectExpression((org.mozilla.javascript.ast.ObjectLiteral) node);
            case Token.CALL, Token.NEW -> convertCallOrNew((FunctionCall) node);
            case Token.GETPROP -> convertPropertyGet((PropertyGet) node);
            case Token.GETELEM -> convertElementGet((ElementGet) node);
            case Token.HOOK ->
                    convertConditionalExpression(
                            (org.mozilla.javascript.ast.ConditionalExpression) node);
            case Token.COMMA ->
                    convertSequenceExpression((org.mozilla.javascript.ast.InfixExpression) node);
            case Token.ASSIGN,
                            Token.ASSIGN_ADD,
                            Token.ASSIGN_SUB,
                            Token.ASSIGN_MUL,
                            Token.ASSIGN_DIV,
                            Token.ASSIGN_MOD,
                            Token.ASSIGN_BITOR,
                            Token.ASSIGN_BITXOR,
                            Token.ASSIGN_BITAND,
                            Token.ASSIGN_LSH,
                            Token.ASSIGN_RSH,
                            Token.ASSIGN_URSH ->
                    convertAssignment((org.mozilla.javascript.ast.Assignment) node);

            // Binary operators
            case Token.OR, Token.AND ->
                    convertLogicalExpression((org.mozilla.javascript.ast.InfixExpression) node);
            case Token.BITOR,
                            Token.BITXOR,
                            Token.BITAND,
                            Token.EQ,
                            Token.NE,
                            Token.LT,
                            Token.LE,
                            Token.GT,
                            Token.GE,
                            Token.LSH,
                            Token.RSH,
                            Token.URSH,
                            Token.ADD,
                            Token.SUB,
                            Token.MUL,
                            Token.DIV,
                            Token.MOD,
                            Token.SHEQ,
                            Token.SHNE,
                            Token.IN,
                            Token.INSTANCEOF ->
                    convertBinaryExpression((org.mozilla.javascript.ast.InfixExpression) node);

            // Unary operators
            case Token.NOT,
                            Token.BITNOT,
                            Token.TYPEOF,
                            Token.VOID,
                            Token.POS,
                            Token.NEG,
                            Token.DELPROP ->
                    convertUnaryExpression((org.mozilla.javascript.ast.UnaryExpression) node);

            // Update operators
            case Token.INC, Token.DEC ->
                    convertUpdateExpression((org.mozilla.javascript.ast.UpdateExpression) node);

            case Token.REGEXP ->
                    convertRegExpLiteral((org.mozilla.javascript.ast.RegExpLiteral) node);

            default ->
                    throw new UnsupportedOperationException(
                            "Unsupported node type: "
                                    + Token.typeToName(tokenType)
                                    + " ("
                                    + node.getClass().getSimpleName()
                                    + ")");
        };
    }

    // ==================== Position Helpers ====================

    private SourceLocation getLocation(AstNode node) {
        int start = node.getAbsolutePosition();
        int end = start + node.getLength();
        return positionConverter.createLocation(start, end);
    }

    private int getStart(AstNode node) {
        return node.getAbsolutePosition();
    }

    private int getEnd(AstNode node) {
        return node.getAbsolutePosition() + node.getLength();
    }

    // ==================== Statement Converters ====================

    private BlockStatement convertBlock(org.mozilla.javascript.ast.Block block) {
        List<Statement> body = new ArrayList<>();

        for (org.mozilla.javascript.Node child : block) {
            if (child instanceof AstNode astNode) {
                Node converted = convert(astNode);
                if (converted instanceof Statement stmt) {
                    body.add(stmt);
                } else {
                    throw new IllegalStateException(
                            "Block body must contain statements, found: "
                                    + converted.getClass().getSimpleName());
                }
            }
        }

        return new BlockStatement(
                getLocation(block),
                getStart(block),
                getEnd(block),
                List.of(),
                List.of(),
                List.of(),
                body);
    }

    private ExpressionStatement convertExpressionStatement(
            org.mozilla.javascript.ast.ExpressionStatement stmt) {
        Expression expression = (Expression) convert(stmt.getExpression());

        return new ExpressionStatement(
                getLocation(stmt),
                getStart(stmt),
                getEnd(stmt),
                List.of(),
                List.of(),
                List.of(),
                expression);
    }

    private IfStatement convertIfStatement(org.mozilla.javascript.ast.IfStatement ifStmt) {
        Expression test = (Expression) convert(ifStmt.getCondition());
        Statement consequent = (Statement) convert(ifStmt.getThenPart());
        Statement alternate =
                ifStmt.getElsePart() != null ? (Statement) convert(ifStmt.getElsePart()) : null;

        return new IfStatement(
                getLocation(ifStmt),
                getStart(ifStmt),
                getEnd(ifStmt),
                List.of(),
                List.of(),
                List.of(),
                test,
                consequent,
                alternate);
    }

    private ReturnStatement convertReturnStatement(
            org.mozilla.javascript.ast.ReturnStatement returnStmt) {
        Expression argument =
                returnStmt.getReturnValue() != null
                        ? (Expression) convert(returnStmt.getReturnValue())
                        : null;

        return new ReturnStatement(
                getLocation(returnStmt),
                getStart(returnStmt),
                getEnd(returnStmt),
                List.of(),
                List.of(),
                List.of(),
                argument);
    }

    private BreakStatement convertBreakStatement(
            org.mozilla.javascript.ast.BreakStatement breakStmt) {
        Identifier label =
                breakStmt.getBreakLabel() != null
                        ? convertIdentifier(breakStmt.getBreakLabel())
                        : null;

        return new BreakStatement(
                getLocation(breakStmt),
                getStart(breakStmt),
                getEnd(breakStmt),
                List.of(),
                List.of(),
                List.of(),
                label);
    }

    private ContinueStatement convertContinueStatement(
            org.mozilla.javascript.ast.ContinueStatement continueStmt) {
        Identifier label =
                continueStmt.getLabel() != null ? convertIdentifier(continueStmt.getLabel()) : null;

        return new ContinueStatement(
                getLocation(continueStmt),
                getStart(continueStmt),
                getEnd(continueStmt),
                List.of(),
                List.of(),
                List.of(),
                label);
    }

    private WhileStatement convertWhileStatement(org.mozilla.javascript.ast.WhileLoop whileLoop) {
        Expression test = (Expression) convert(whileLoop.getCondition());
        Statement body = (Statement) convert(whileLoop.getBody());

        return new WhileStatement(
                getLocation(whileLoop),
                getStart(whileLoop),
                getEnd(whileLoop),
                List.of(),
                List.of(),
                List.of(),
                test,
                body);
    }

    private DoWhileStatement convertDoWhileStatement(org.mozilla.javascript.ast.DoLoop doLoop) {
        Statement body = (Statement) convert(doLoop.getBody());
        Expression test = (Expression) convert(doLoop.getCondition());

        return new DoWhileStatement(
                getLocation(doLoop),
                getStart(doLoop),
                getEnd(doLoop),
                List.of(),
                List.of(),
                List.of(),
                body,
                test);
    }

    private ForStatement convertForStatement(org.mozilla.javascript.ast.ForLoop forLoop) {
        Node init = forLoop.getInitializer() != null ? convert(forLoop.getInitializer()) : null;
        Expression test =
                forLoop.getCondition() != null
                        ? (Expression) convert(forLoop.getCondition())
                        : null;
        Expression update =
                forLoop.getIncrement() != null
                        ? (Expression) convert(forLoop.getIncrement())
                        : null;
        Statement body = (Statement) convert(forLoop.getBody());

        // Init can be VariableDeclaration or Expression
        if (init != null
                && !(init instanceof VariableDeclaration)
                && !(init instanceof Expression)) {
            throw new IllegalStateException(
                    "For loop init must be VariableDeclaration or Expression, found: "
                            + init.getClass().getSimpleName());
        }

        return new ForStatement(
                getLocation(forLoop),
                getStart(forLoop),
                getEnd(forLoop),
                List.of(),
                List.of(),
                List.of(),
                init,
                test,
                update,
                body);
    }

    private ForInStatement convertForInStatement(org.mozilla.javascript.ast.ForInLoop forInLoop) {
        Node left = convert(forInLoop.getIterator());
        Expression right = (Expression) convert(forInLoop.getIteratedObject());
        Statement body = (Statement) convert(forInLoop.getBody());

        // Left can be VariableDeclaration or Pattern
        if (!(left instanceof VariableDeclaration) && !(left instanceof Pattern)) {
            throw new IllegalStateException(
                    "For-in loop left must be VariableDeclaration or Pattern, found: "
                            + left.getClass().getSimpleName());
        }

        return new ForInStatement(
                getLocation(forInLoop),
                getStart(forInLoop),
                getEnd(forInLoop),
                List.of(),
                List.of(),
                List.of(),
                left,
                right,
                body);
    }

    private SwitchStatement convertSwitchStatement(
            org.mozilla.javascript.ast.SwitchStatement switchStmt) {
        Expression discriminant = (Expression) convert(switchStmt.getExpression());
        List<SwitchCase> cases = new ArrayList<>();

        for (org.mozilla.javascript.ast.SwitchCase rhinoCase : switchStmt.getCases()) {
            cases.add(convertSwitchCase(rhinoCase));
        }

        return new SwitchStatement(
                getLocation(switchStmt),
                getStart(switchStmt),
                getEnd(switchStmt),
                List.of(),
                List.of(),
                List.of(),
                discriminant,
                cases);
    }

    private SwitchCase convertSwitchCase(org.mozilla.javascript.ast.SwitchCase rhinoCase) {
        Expression test =
                rhinoCase.getExpression() != null
                        ? (Expression) convert(rhinoCase.getExpression())
                        : null; // null for default case

        List<Statement> consequent = new ArrayList<>();
        if (rhinoCase.getStatements() != null) {
            for (AstNode stmt : rhinoCase.getStatements()) {
                Node converted = convert(stmt);
                if (converted instanceof Statement statement) {
                    consequent.add(statement);
                }
            }
        }

        return new SwitchCase(
                getLocation(rhinoCase),
                getStart(rhinoCase),
                getEnd(rhinoCase),
                List.of(),
                List.of(),
                List.of(),
                test,
                consequent);
    }

    private ThrowStatement convertThrowStatement(
            org.mozilla.javascript.ast.ThrowStatement throwStmt) {
        Expression argument = (Expression) convert(throwStmt.getExpression());

        return new ThrowStatement(
                getLocation(throwStmt),
                getStart(throwStmt),
                getEnd(throwStmt),
                List.of(),
                List.of(),
                List.of(),
                argument);
    }

    private TryStatement convertTryStatement(org.mozilla.javascript.ast.TryStatement tryStmt) {
        BlockStatement block =
                convertBlock((org.mozilla.javascript.ast.Block) tryStmt.getTryBlock());

        // ESTree supports only a single catch clause, take the first one if multiple
        CatchClause handler = null;
        List<org.mozilla.javascript.ast.CatchClause> catchClauses = tryStmt.getCatchClauses();
        if (!catchClauses.isEmpty()) {
            handler = convertCatchClause(catchClauses.get(0));
        }

        BlockStatement finalizer =
                tryStmt.getFinallyBlock() != null
                        ? convertBlock((org.mozilla.javascript.ast.Block) tryStmt.getFinallyBlock())
                        : null;

        return new TryStatement(
                getLocation(tryStmt),
                getStart(tryStmt),
                getEnd(tryStmt),
                List.of(),
                List.of(),
                List.of(),
                block,
                handler,
                finalizer);
    }

    private CatchClause convertCatchClause(org.mozilla.javascript.ast.CatchClause rhinoClause) {
        Pattern param =
                rhinoClause.getVarName() != null
                        ? (Pattern) convert(rhinoClause.getVarName())
                        : null;

        // getBody() returns a Scope - convert it to BlockStatement by iterating children
        org.mozilla.javascript.ast.Scope scopeBody = rhinoClause.getBody();
        List<Statement> statements = new ArrayList<>();
        for (org.mozilla.javascript.Node child : scopeBody) {
            if (child instanceof AstNode astNode) {
                Node converted = convert(astNode);
                if (converted instanceof Statement stmt) {
                    statements.add(stmt);
                }
            }
        }
        BlockStatement body =
                new BlockStatement(
                        getLocation(scopeBody),
                        getStart(scopeBody),
                        getEnd(scopeBody),
                        List.of(),
                        List.of(),
                        List.of(),
                        statements);

        return new CatchClause(
                getLocation(rhinoClause),
                getStart(rhinoClause),
                getEnd(rhinoClause),
                List.of(),
                List.of(),
                List.of(),
                param,
                body);
    }

    private WithStatement convertWithStatement(org.mozilla.javascript.ast.WithStatement withStmt) {
        Expression object = (Expression) convert(withStmt.getExpression());
        Statement body = (Statement) convert(withStmt.getStatement());

        return new WithStatement(
                getLocation(withStmt),
                getStart(withStmt),
                getEnd(withStmt),
                List.of(),
                List.of(),
                List.of(),
                object,
                body);
    }

    private LabeledStatement convertLabeledStatement(
            org.mozilla.javascript.ast.LabeledStatement labeledStmt) {
        // Rhino Label has getName(), create an Identifier from it
        org.mozilla.javascript.ast.Label rhinoLabel = labeledStmt.getLabels().get(0);
        Identifier label =
                new Identifier(
                        getLocation(rhinoLabel),
                        getStart(rhinoLabel),
                        getEnd(rhinoLabel),
                        List.of(),
                        List.of(),
                        List.of(),
                        rhinoLabel.getName());
        Statement body = (Statement) convert(labeledStmt.getStatement());

        return new LabeledStatement(
                getLocation(labeledStmt),
                getStart(labeledStmt),
                getEnd(labeledStmt),
                List.of(),
                List.of(),
                List.of(),
                label,
                body);
    }

    private DebuggerStatement convertDebuggerStatement(
            org.mozilla.javascript.ast.KeywordLiteral debugger) {
        return new DebuggerStatement(
                getLocation(debugger),
                getStart(debugger),
                getEnd(debugger),
                List.of(),
                List.of(),
                List.of());
    }

    private EmptyStatement convertEmptyStatement(org.mozilla.javascript.ast.EmptyStatement empty) {
        return new EmptyStatement(
                getLocation(empty),
                getStart(empty),
                getEnd(empty),
                List.of(),
                List.of(),
                List.of());
    }

    // ==================== Declaration Converters ====================

    private VariableDeclaration convertVariableDeclaration(
            org.mozilla.javascript.ast.VariableDeclaration varDecl) {
        VariableDeclarationKind kind =
                switch (varDecl.getType()) {
                    case Token.VAR -> VariableDeclarationKind.VAR;
                    case Token.LET -> VariableDeclarationKind.LET;
                    case Token.CONST -> VariableDeclarationKind.CONST;
                    default -> throw new IllegalStateException("Unknown variable declaration kind");
                };

        List<VariableDeclarator> declarations = new ArrayList<>();
        for (org.mozilla.javascript.ast.VariableInitializer varInit : varDecl.getVariables()) {
            declarations.add(convertVariableDeclarator(varInit));
        }

        return new VariableDeclaration(
                getLocation(varDecl),
                getStart(varDecl),
                getEnd(varDecl),
                List.of(),
                List.of(),
                List.of(),
                declarations,
                kind);
    }

    private VariableDeclarator convertVariableDeclarator(
            org.mozilla.javascript.ast.VariableInitializer varInit) {
        Pattern id = (Pattern) convert(varInit.getTarget());
        Expression init =
                varInit.getInitializer() != null
                        ? (Expression) convert(varInit.getInitializer())
                        : null;

        return new VariableDeclarator(
                getLocation(varInit),
                getStart(varInit),
                getEnd(varInit),
                List.of(),
                List.of(),
                List.of(),
                id,
                init);
    }

    private Node convertFunction(AstNode node) {
        org.mozilla.javascript.ast.FunctionNode funcNode =
                (org.mozilla.javascript.ast.FunctionNode) node;

        List<Pattern> params = new ArrayList<>();
        for (AstNode param : funcNode.getParams()) {
            params.add((Pattern) convert(param));
        }

        BlockStatement body = convertBlock((org.mozilla.javascript.ast.Block) funcNode.getBody());

        Identifier id =
                funcNode.getFunctionName() != null
                        ? convertIdentifier(funcNode.getFunctionName())
                        : null;

        boolean generator = funcNode.isGenerator();
        // TODO: isAsync() not available in current Rhino version
        boolean async = false;

        // Determine if it's a declaration or expression
        int funcType = funcNode.getFunctionType();
        if (funcType == org.mozilla.javascript.ast.FunctionNode.FUNCTION_STATEMENT) {
            return new FunctionDeclaration(
                    getLocation(funcNode),
                    getStart(funcNode),
                    getEnd(funcNode),
                    List.of(),
                    List.of(),
                    List.of(),
                    id,
                    params,
                    body,
                    generator,
                    async);
        } else {
            return new FunctionExpression(
                    getLocation(funcNode),
                    getStart(funcNode),
                    getEnd(funcNode),
                    List.of(),
                    List.of(),
                    List.of(),
                    id,
                    params,
                    body,
                    generator,
                    async);
        }
    }

    // ==================== Expression Converters ====================

    private Identifier convertIdentifier(Name name) {
        return new Identifier(
                getLocation(name),
                getStart(name),
                getEnd(name),
                List.of(),
                List.of(),
                List.of(),
                name.getIdentifier());
    }

    private NumberLiteral convertNumberLiteral(org.mozilla.javascript.ast.NumberLiteral num) {
        return new NumberLiteral(
                getLocation(num),
                getStart(num),
                getEnd(num),
                List.of(),
                List.of(),
                List.of(),
                num.getNumber(),
                num.getValue());
    }

    private StringLiteral convertStringLiteral(org.mozilla.javascript.ast.StringLiteral str) {
        return new StringLiteral(
                getLocation(str),
                getStart(str),
                getEnd(str),
                List.of(),
                List.of(),
                List.of(),
                str.getValue(),
                "\"" + str.getValue() + "\""); // Add quotes for raw representation
    }

    private SimpleLiteral convertKeywordLiteral(org.mozilla.javascript.ast.KeywordLiteral kw) {
        return switch (kw.getType()) {
            case Token.TRUE ->
                    new BooleanLiteral(
                            getLocation(kw),
                            getStart(kw),
                            getEnd(kw),
                            List.of(),
                            List.of(),
                            List.of(),
                            true,
                            "true");
            case Token.FALSE ->
                    new BooleanLiteral(
                            getLocation(kw),
                            getStart(kw),
                            getEnd(kw),
                            List.of(),
                            List.of(),
                            List.of(),
                            false,
                            "false");
            case Token.NULL ->
                    new NullLiteral(
                            getLocation(kw),
                            getStart(kw),
                            getEnd(kw),
                            List.of(),
                            List.of(),
                            List.of(),
                            "null");
            default -> throw new IllegalStateException("Unknown keyword literal");
        };
    }

    private ThisExpression convertThisExpression(org.mozilla.javascript.ast.KeywordLiteral kw) {
        return new ThisExpression(
                getLocation(kw), getStart(kw), getEnd(kw), List.of(), List.of(), List.of());
    }

    private ArrayExpression convertArrayExpression(org.mozilla.javascript.ast.ArrayLiteral array) {
        List<Expression> elements = new ArrayList<>();

        for (AstNode elem : array.getElements()) {
            // EmptyExpression represents holes in sparse arrays
            if (elem instanceof org.mozilla.javascript.ast.EmptyExpression) {
                elements.add(null); // null represents a hole
            } else {
                elements.add((Expression) convert(elem));
            }
        }

        return new ArrayExpression(
                getLocation(array),
                getStart(array),
                getEnd(array),
                List.of(),
                List.of(),
                List.of(),
                elements);
    }

    private ObjectExpression convertObjectExpression(
            org.mozilla.javascript.ast.ObjectLiteral object) {
        List<Property> properties = new ArrayList<>();

        for (org.mozilla.javascript.ast.AbstractObjectProperty abstractProp :
                object.getElements()) {
            // Only handle ObjectProperty for now, not getters/setters
            if (abstractProp instanceof org.mozilla.javascript.ast.ObjectProperty prop) {
                properties.add(convertProperty(prop));
            } else {
                throw new UnsupportedOperationException(
                        "Unsupported object property type: "
                                + abstractProp.getClass().getSimpleName());
            }
        }

        return new ObjectExpression(
                getLocation(object),
                getStart(object),
                getEnd(object),
                List.of(),
                List.of(),
                List.of(),
                properties);
    }

    private Property convertProperty(org.mozilla.javascript.ast.ObjectProperty prop) {
        Expression key;
        AstNode keyNode = prop.getKey();

        // Key can be Name, String, or Number
        if (keyNode instanceof Name name) {
            key = convertIdentifier(name);
        } else if (keyNode instanceof org.mozilla.javascript.ast.StringLiteral str) {
            key = convertStringLiteral(str);
        } else if (keyNode instanceof org.mozilla.javascript.ast.NumberLiteral num) {
            key = convertNumberLiteral(num);
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported property key type: " + keyNode.getClass().getSimpleName());
        }

        Expression value = (Expression) convert(prop.getValue());

        // For now, all properties are "init" kind (not getter/setter)
        String kind = "init"; // TODO: handle getters/setters in future phases

        // Computed is true for bracket notation: {[expr]: value}
        boolean computed = false; // TODO: detect computed property names

        // Shorthand is true for {x} instead of {x: x}
        boolean shorthand = false; // TODO: detect shorthand properties

        // Method is true for {foo() {}}
        boolean method = false; // TODO: detect method properties

        return new Property(
                getLocation(prop),
                getStart(prop),
                getEnd(prop),
                List.of(),
                List.of(),
                List.of(),
                key,
                value,
                kind,
                method,
                shorthand,
                computed);
    }

    private Node convertCallOrNew(FunctionCall call) {
        Expression callee = (Expression) convert(call.getTarget());
        List<Expression> arguments = new ArrayList<>();

        for (AstNode arg : call.getArguments()) {
            arguments.add((Expression) convert(arg));
        }

        if (call instanceof org.mozilla.javascript.ast.NewExpression) {
            return new NewExpression(
                    getLocation(call),
                    getStart(call),
                    getEnd(call),
                    List.of(),
                    List.of(),
                    List.of(),
                    callee,
                    arguments);
        } else {
            return new CallExpression(
                    getLocation(call),
                    getStart(call),
                    getEnd(call),
                    List.of(),
                    List.of(),
                    List.of(),
                    callee,
                    arguments,
                    false); // optional (TODO: handle optional chaining)
        }
    }

    private MemberExpression convertPropertyGet(PropertyGet propGet) {
        Expression object = (Expression) convert(propGet.getTarget());
        Expression property = convertIdentifier(propGet.getProperty());

        return new MemberExpression(
                getLocation(propGet),
                getStart(propGet),
                getEnd(propGet),
                List.of(),
                List.of(),
                List.of(),
                object,
                property,
                false, // computed (dot notation is not computed)
                false); // optional (TODO: handle optional chaining)
    }

    private MemberExpression convertElementGet(ElementGet elemGet) {
        Expression object = (Expression) convert(elemGet.getTarget());
        Expression property = (Expression) convert(elemGet.getElement());

        return new MemberExpression(
                getLocation(elemGet),
                getStart(elemGet),
                getEnd(elemGet),
                List.of(),
                List.of(),
                List.of(),
                object,
                property,
                true, // computed (bracket notation is computed)
                false); // optional
    }

    private ConditionalExpression convertConditionalExpression(
            org.mozilla.javascript.ast.ConditionalExpression cond) {
        Expression test = (Expression) convert(cond.getTestExpression());
        Expression consequent = (Expression) convert(cond.getTrueExpression());
        Expression alternate = (Expression) convert(cond.getFalseExpression());

        return new ConditionalExpression(
                getLocation(cond),
                getStart(cond),
                getEnd(cond),
                List.of(),
                List.of(),
                List.of(),
                test,
                consequent,
                alternate);
    }

    private SequenceExpression convertSequenceExpression(
            org.mozilla.javascript.ast.InfixExpression seq) {
        // Comma operator creates a sequence - need to flatten nested commas
        List<Expression> expressions = new ArrayList<>();
        collectSequenceExpressions(seq, expressions);

        return new SequenceExpression(
                getLocation(seq),
                getStart(seq),
                getEnd(seq),
                List.of(),
                List.of(),
                List.of(),
                expressions);
    }

    private void collectSequenceExpressions(
            org.mozilla.javascript.ast.InfixExpression expr, List<Expression> result) {
        // Left side
        if (expr.getLeft() instanceof org.mozilla.javascript.ast.InfixExpression left
                && left.getType() == Token.COMMA) {
            collectSequenceExpressions(left, result);
        } else {
            result.add((Expression) convert(expr.getLeft()));
        }

        // Right side
        if (expr.getRight() instanceof org.mozilla.javascript.ast.InfixExpression right
                && right.getType() == Token.COMMA) {
            collectSequenceExpressions(right, result);
        } else {
            result.add((Expression) convert(expr.getRight()));
        }
    }

    private AssignmentExpression convertAssignment(org.mozilla.javascript.ast.Assignment assign) {
        AssignmentOperator operator = getAssignmentOperator(assign.getType());
        Pattern left = (Pattern) convert(assign.getLeft());
        Expression right = (Expression) convert(assign.getRight());

        return new AssignmentExpression(
                getLocation(assign),
                getStart(assign),
                getEnd(assign),
                List.of(),
                List.of(),
                List.of(),
                operator,
                left,
                right);
    }

    private AssignmentOperator getAssignmentOperator(int tokenType) {
        return switch (tokenType) {
            case Token.ASSIGN -> AssignmentOperator.ASSIGN;
            case Token.ASSIGN_ADD -> AssignmentOperator.ADD_ASSIGN;
            case Token.ASSIGN_SUB -> AssignmentOperator.SUB_ASSIGN;
            case Token.ASSIGN_MUL -> AssignmentOperator.MUL_ASSIGN;
            case Token.ASSIGN_DIV -> AssignmentOperator.DIV_ASSIGN;
            case Token.ASSIGN_MOD -> AssignmentOperator.MOD_ASSIGN;
            case Token.ASSIGN_BITOR -> AssignmentOperator.BITOR_ASSIGN;
            case Token.ASSIGN_BITXOR -> AssignmentOperator.BITXOR_ASSIGN;
            case Token.ASSIGN_BITAND -> AssignmentOperator.BITAND_ASSIGN;
            case Token.ASSIGN_LSH -> AssignmentOperator.LSH_ASSIGN;
            case Token.ASSIGN_RSH -> AssignmentOperator.RSH_ASSIGN;
            case Token.ASSIGN_URSH -> AssignmentOperator.URSH_ASSIGN;
            default -> throw new IllegalArgumentException("Unknown assignment operator");
        };
    }

    private LogicalExpression convertLogicalExpression(
            org.mozilla.javascript.ast.InfixExpression logical) {
        LogicalOperator operator =
                logical.getType() == Token.OR ? LogicalOperator.OR : LogicalOperator.AND;
        Expression left = (Expression) convert(logical.getLeft());
        Expression right = (Expression) convert(logical.getRight());

        return new LogicalExpression(
                getLocation(logical),
                getStart(logical),
                getEnd(logical),
                List.of(),
                List.of(),
                List.of(),
                operator,
                left,
                right);
    }

    private BinaryExpression convertBinaryExpression(
            org.mozilla.javascript.ast.InfixExpression binary) {
        BinaryOperator operator = getBinaryOperator(binary.getType());
        Expression left = (Expression) convert(binary.getLeft());
        Expression right = (Expression) convert(binary.getRight());

        return new BinaryExpression(
                getLocation(binary),
                getStart(binary),
                getEnd(binary),
                List.of(),
                List.of(),
                List.of(),
                operator,
                left,
                right);
    }

    private BinaryOperator getBinaryOperator(int tokenType) {
        return switch (tokenType) {
            case Token.BITOR -> BinaryOperator.BITOR;
            case Token.BITXOR -> BinaryOperator.BITXOR;
            case Token.BITAND -> BinaryOperator.BITAND;
            case Token.EQ -> BinaryOperator.EQ;
            case Token.NE -> BinaryOperator.NE;
            case Token.LT -> BinaryOperator.LT;
            case Token.LE -> BinaryOperator.LE;
            case Token.GT -> BinaryOperator.GT;
            case Token.GE -> BinaryOperator.GE;
            case Token.LSH -> BinaryOperator.LSH;
            case Token.RSH -> BinaryOperator.RSH;
            case Token.URSH -> BinaryOperator.URSH;
            case Token.ADD -> BinaryOperator.ADD;
            case Token.SUB -> BinaryOperator.SUB;
            case Token.MUL -> BinaryOperator.MUL;
            case Token.DIV -> BinaryOperator.DIV;
            case Token.MOD -> BinaryOperator.MOD;
            case Token.SHEQ -> BinaryOperator.STRICT_EQ;
            case Token.SHNE -> BinaryOperator.STRICT_NE;
            case Token.IN -> BinaryOperator.IN;
            case Token.INSTANCEOF -> BinaryOperator.INSTANCEOF;
            default -> throw new IllegalArgumentException("Unknown binary operator");
        };
    }

    private UnaryExpression convertUnaryExpression(
            org.mozilla.javascript.ast.UnaryExpression unary) {
        UnaryOperator operator = getUnaryOperator(unary.getType());
        Expression argument = (Expression) convert(unary.getOperand());

        return new UnaryExpression(
                getLocation(unary),
                getStart(unary),
                getEnd(unary),
                List.of(),
                List.of(),
                List.of(),
                operator,
                true, // prefix (all unary operators are prefix except ++ and --)
                argument);
    }

    private UnaryOperator getUnaryOperator(int tokenType) {
        return switch (tokenType) {
            case Token.NOT -> UnaryOperator.NOT;
            case Token.BITNOT -> UnaryOperator.BITWISE_NOT;
            case Token.TYPEOF -> UnaryOperator.TYPEOF;
            case Token.VOID -> UnaryOperator.VOID;
            case Token.POS -> UnaryOperator.PLUS;
            case Token.NEG -> UnaryOperator.MINUS;
            case Token.DELPROP -> UnaryOperator.DELETE;
            default -> throw new IllegalArgumentException("Unknown unary operator");
        };
    }

    private UpdateExpression convertUpdateExpression(
            org.mozilla.javascript.ast.UpdateExpression update) {
        UpdateOperator operator =
                update.getType() == Token.INC ? UpdateOperator.INCREMENT : UpdateOperator.DECREMENT;
        Expression argument = (Expression) convert(update.getOperand());
        boolean prefix = !update.isPostfix();

        return new UpdateExpression(
                getLocation(update),
                getStart(update),
                getEnd(update),
                List.of(),
                List.of(),
                List.of(),
                operator,
                argument,
                prefix);
    }

    private RegExpLiteral convertRegExpLiteral(org.mozilla.javascript.ast.RegExpLiteral regexp) {
        String value = regexp.getValue();
        String flags = regexp.getFlags();

        // ESTree expects pattern and flags separately
        // Rhino stores them as "/pattern/flags", so we need to parse it
        String pattern;
        if (value.startsWith("/")) {
            int lastSlash = value.lastIndexOf('/');
            pattern = value.substring(1, lastSlash);
        } else {
            pattern = value;
        }

        return new RegExpLiteral(
                getLocation(regexp),
                getStart(regexp),
                getEnd(regexp),
                List.of(),
                List.of(),
                List.of(),
                null, // value is always null for regex
                value, // raw
                new RegExpLiteral.RegExpValue(pattern, flags != null ? flags : ""));
    }
}
