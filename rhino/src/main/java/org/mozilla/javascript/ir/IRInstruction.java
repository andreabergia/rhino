package org.mozilla.javascript.ir;

record Span(int line, int column) {}

public sealed interface IRInstruction {
    // TODO
    //	Span span();

    record Name(String name) implements IRInstruction {}

    record PushConstant(ConstantValue value) implements IRInstruction {}

    record Add() implements IRInstruction {}

    record Sub() implements IRInstruction {}

    record Mul() implements IRInstruction {}

    record Div() implements IRInstruction {}

    record Neg() implements IRInstruction {}

    record Not() implements IRInstruction {}

    record PopResult() implements IRInstruction {}

    record Typeof() implements IRInstruction {}
}
