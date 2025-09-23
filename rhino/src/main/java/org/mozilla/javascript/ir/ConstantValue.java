package org.mozilla.javascript.ir;

public sealed interface ConstantValue {
    record ConstantShort(short value) implements ConstantValue {}

    record ConstantInt(int value) implements ConstantValue {}

    record ConstantDouble(double value) implements ConstantValue {}

    record ConstantBoolean(boolean value) implements ConstantValue {}

    record ConstantString(String value) implements ConstantValue {}

    record ConstantNull() implements ConstantValue {}

    record ConstantUndefined() implements ConstantValue {}
}
