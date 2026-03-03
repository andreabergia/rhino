/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

/** Binary operators used in {@link Instruction.BinaryOp}. */
public enum BinOp {
    ADD,
    SUB,
    MUL,
    DIV,
    MOD,
    EXP,
    BITOR,
    BITXOR,
    BITAND,
    LSH,
    RSH,
    URSH,
    EQ,
    NE,
    SHEQ,
    SHNE,
    LT,
    LE,
    GT,
    GE,
    IN,
    INSTANCEOF
}
