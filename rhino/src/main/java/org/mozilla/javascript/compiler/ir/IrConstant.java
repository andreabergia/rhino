/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import java.math.BigInteger;

/** Constant values that can appear in IR instructions. */
public sealed interface IrConstant {
    record NumberConst(double value) implements IrConstant {}

    record StringConst(String value) implements IrConstant {}

    record BigIntConst(BigInteger value) implements IrConstant {}

    record BooleanConst(boolean value) implements IrConstant {}

    record NullConst() implements IrConstant {}

    record UndefinedConst() implements IrConstant {}

    record RegExpConst(String pattern, String flags) implements IrConstant {}
}
