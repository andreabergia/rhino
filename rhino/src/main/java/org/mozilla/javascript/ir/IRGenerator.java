package org.mozilla.javascript.ir;

import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.UnaryExpression;

public class IRGenerator {
    private List<IRInstruction> current;

    IRGenerator() {}

    public IRScript generate(AstRoot root) {
        if (Token.printTrees) {
            System.out.println("IRGenerator.generate:");
            System.out.println(root.debugPrint());
        }

        current = new ArrayList<>();

        Node r = root.getFirstChild();
        if (r instanceof ExpressionStatement e) {
            transformExpressionStatement(e);
        } else {
            throw new UnsupportedOperationException("TODO: " + r);
        }

        return new IRScript(current);
    }

    private void transformExpressionStatement(ExpressionStatement e) {
        transformExpression(e.getExpression());
        current.add(new IRInstruction.PopResult());
    }

    private void transformExpression(AstNode expression) {
        if (expression instanceof InfixExpression infix) {
            transformExpressionInfix(infix);
        } else if (expression instanceof UnaryExpression unary) {
            transformExpressionUnary(unary);
        } else if (expression instanceof NumberLiteral lit) {
            transformNumberLiteral(lit);
        } else if (expression instanceof KeywordLiteral lit) {
            transformKeywordLiteral(lit);
        } else if (expression instanceof Name name) {
            transformName(name);
        } else if (expression instanceof ParenthesizedExpression pe) {
            transformParenthesizedExpression(pe);
        } else {
            throw new UnsupportedOperationException(
                    "TODO: " + Token.typeToName(expression.getType()) + " - " + expression);
        }
    }

    private void transformExpressionInfix(InfixExpression infix) {
        transformExpression(infix.getLeft());
        transformExpression(infix.getRight());

        switch (infix.getOperator()) {
            case Token.ADD -> current.add(new IRInstruction.Add());
            case Token.SUB -> current.add(new IRInstruction.Sub());
            case Token.MUL -> current.add(new IRInstruction.Mul());
            case Token.DIV -> current.add(new IRInstruction.Div());
            default ->
                    throw new UnsupportedOperationException(
                            "TODO: " + Token.typeToName(infix.getOperator()));
        }
    }

    private void transformExpressionUnary(UnaryExpression unary) {
        transformExpression(unary.getOperand());

        switch (unary.getOperator()) {
            case Token.NOT -> current.add(new IRInstruction.Not());
            case Token.NEG -> current.add(new IRInstruction.Neg());
            case Token.TYPEOF -> current.add(new IRInstruction.Typeof());
            default ->
                    throw new UnsupportedOperationException(
                            "TODO: " + Token.typeToName(unary.getOperator()));
        }
    }

    private void transformNumberLiteral(NumberLiteral lit) {
        double f64 = lit.getNumber();
        int i32 = ScriptRuntime.toInt32(f64);
        if (i32 == f64) {
            current.add(new IRInstruction.PushConstant(new ConstantValue.ConstantInt(i32)));
        } else {
            current.add(new IRInstruction.PushConstant(new ConstantValue.ConstantDouble(f64)));
        }
    }

    private void transformKeywordLiteral(KeywordLiteral lit) {
        switch (lit.getType()) {
            case Token.TRUE ->
                    current.add(
                            new IRInstruction.PushConstant(
                                    new ConstantValue.ConstantBoolean(true)));
            case Token.FALSE ->
                    current.add(
                            new IRInstruction.PushConstant(
                                    new ConstantValue.ConstantBoolean(false)));
            case Token.NULL ->
                    current.add(new IRInstruction.PushConstant(new ConstantValue.ConstantNull()));
            case Token.UNDEFINED ->
                    current.add(
                            new IRInstruction.PushConstant(new ConstantValue.ConstantUndefined()));
            default ->
                    throw new UnsupportedOperationException(
                            "TODO: " + Token.typeToName(lit.getType()));
        }
    }

    private void transformName(Name name) {
        // TODO: if it's an argument, we'd like to treat this differently
        current.add(new IRInstruction.Name(name.getIdentifier()));
    }

    private void transformParenthesizedExpression(ParenthesizedExpression pe) {
        transformExpression(pe.getExpression());
    }
}
