/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.debug.DebuggableScript;

/** Abstraction of evaluation, which can be implemented either by an interpreter or compiler. */
public interface Evaluator {

    /**
     * Compile the script or function from intermediate representation tree into an executable form.
     *
     * @param compilerEnv Compiler environment
     * @param tree parse tree
     * @param rawSource the source code
     * @param returnFunction if true, compiling a function
     * @return an opaque object that can be passed to either createFunctionObject or
     *     createScriptObject, depending on the value of returnFunction
     */
    public Object compile(
            CompilerEnvirons compilerEnv,
            ScriptNode tree,
            String rawSource,
            boolean returnFunction);

    /**
     * Create a function object.
     *
     * @param cx Current context
     * @param scope scope of the function
     * @param bytecode opaque object returned by compile
     * @param staticSecurityDomain security domain
     * @return Function object that can be called
     */
    public Function createFunctionObject(
            Context cx, Scriptable scope, Object bytecode, Object staticSecurityDomain);

    /**
     * Create a script object.
     *
     * @param bytecode opaque object returned by compile
     * @param staticSecurityDomain security domain
     * @return Script object that can be evaluated
     */
    public Script createScriptObject(Object bytecode, Object staticSecurityDomain);

    /**
     * Mark the given script to indicate it was created by a call to eval() or to a Function
     * constructor.
     *
     * @param script script to mark as from eval
     */
    public void setEvalScriptFlag(Script script);

    public default DebuggableScript getDebuggableScript(Object bytecode) {
        return null;
    }
}
