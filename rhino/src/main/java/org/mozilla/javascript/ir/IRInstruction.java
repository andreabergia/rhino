package org.mozilla.javascript.ir;

record Span(int line, int column) {}

public sealed interface IRInstruction {
    // TODO
    //	Span span();

    record Name(String name) implements IRInstruction {}

    record PushConstant(ConstantValue value) implements IRInstruction {}

    record Unary(UnaryOperator op) implements IRInstruction {}

    record Binary(BinaryOperator op) implements IRInstruction {}

    record PopResult() implements IRInstruction {}

    enum UnaryOperator {
        Neg,
        Not,
        Typeof,
    }

    enum BinaryOperator {
        Add,
        Sub,
        Mul,
        Div,
        ShallowEq,
        Eq,
        ShallowNe,
        Ne,
        Lt,
        Le,
        Gt,
        Ge,
    }
}
