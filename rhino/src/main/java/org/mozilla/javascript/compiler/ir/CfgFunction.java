/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import java.util.List;

/** Top-level container for a function or script compiled to CFG-based IR. */
public record CfgFunction(
        String name,
        String sourceName,
        int baseLineNumber,
        int endLineNumber,
        int paramCount,
        String[] variableNames,
        boolean[] isConst,
        boolean isStrict,
        boolean isGenerator,
        boolean isES6Generator,
        boolean isArrow,
        boolean isMethod,
        boolean isExpressionClosure,
        boolean needsActivation,
        boolean hasRestParameter,
        FunctionKind functionKind,
        BlockId entryBlock,
        List<BasicBlock> blocks,
        List<ExceptionHandler> exceptionHandlers,
        List<CfgFunction> nestedFunctions,
        int registerCount,
        boolean isSsa) {}
