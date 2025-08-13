package org.mozilla.javascript;

public class NativeClass extends BaseFunction {
    private final NativeFunction constructor;

    public NativeClass(NativeFunction constructor) {
        this.constructor = constructor;
    }

    public NativeFunction getConstructor() {
        return constructor;
    }

    static NativeClass createClass(
            Context cx, Scriptable scope, InterpretedFunction parent, InterpreterClassData icd) {
        // TODO: what else? properties, extends?
        InterpretedFunction constructor =
                InterpretedFunction.createFunction(
                        cx, scope, parent, icd.getConstructorFunctionId());

        NativeClass nc = new NativeClass(constructor);
        nc.put("constructor", nc, constructor);

        // TODO: if no extends, this means we have the Function.prototype
        ScriptRuntime.setBuiltinProtoAndParent(nc, scope, TopLevel.Builtins.Function);
        nc.setPrototypeProperty(constructor.getPrototypeProperty());

        // Store in scope
        String functionName = constructor.getFunctionName();
        if (functionName != null && !functionName.isEmpty()) {
            scope.put(functionName, scope, nc);
        }

        return nc;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return constructor.call(cx, scope, thisObj, args);
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        return constructor.construct(cx, scope, args);
    }

    //	@Override
    //	public String getClassName() {
    //		throw new UnsupportedOperationException("TODO");
    //	}
}
