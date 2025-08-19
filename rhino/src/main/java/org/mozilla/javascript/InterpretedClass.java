package org.mozilla.javascript;

// TODO: it really is not great that this inherits from InterpretedFunction, frankly.
//  But the interpreter seems TOO tied to InterpretedFunction to change this, at least for the moment. However, we're going to have a hard time for compiled mode with this approach...
public class InterpretedClass extends InterpretedFunction {
    private InterpretedClass(InterpretedFunction parent, InterpreterData idata) {
        super(idata, parent);
    }

    public static InterpretedClass createClass(
            Context cx, Scriptable scope, InterpretedFunction parent, InterpreterData idata) {
        InterpretedClass f = new InterpretedClass(parent, idata);

        f.setParentScope(scope);

        // TODO: inheritance
        f.setPrototype(ScriptableObject.getFunctionPrototype(scope));
        f.setupDefaultPrototype(scope);

        // TODO: should not have "arity"
        f.setStandardPropertyAttributes(ScriptableObject.READONLY | ScriptableObject.DONTENUM);

        return f;
    }

    @Override
    public final Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        throw ScriptRuntime.typeErrorById("msg.class.constructor.needs.new", getFunctionName());
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        // TODO: simplified version of "construct" from BaseFunction

        Scriptable result = createObject(cx, scope);
        assert result != null; // TODO

	    // Inline super.call basically
        Object val = Interpreter.interpret(this, cx, scope, result, args);
        if (val instanceof Scriptable) {
            result = (Scriptable) val;
        }

        return result;
    }
}
