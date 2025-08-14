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
        InterpretedFunction constructor =
                InterpretedFunction.createFunction(
                        cx, scope, parent, icd.getConstructorFunctionId());

        NativeClass nc = new NativeClass(constructor);
        nc.put("constructor", nc, constructor);

        // TODO: if no extends, this means we have the Function.prototype
        ScriptRuntime.setBuiltinProtoAndParent(nc, scope, TopLevel.Builtins.Function);

        Scriptable prototypeProperty = getPrototypePropertyAsScriptable(constructor);
        nc.setPrototypeProperty(prototypeProperty);

        // Members
        for (Integer memberFunctionId : icd.getMemberFunctionIds()) {
            InterpretedFunction member =
                    InterpretedFunction.createFunction(cx, scope, parent, memberFunctionId);
            String memberName = member.getFunctionName();
            assert memberName != null && !memberName.isEmpty();
            prototypeProperty.put(
                    memberName, prototypeProperty, member); // Members go to the prototype property
        }
        for (Integer staticFunctionId : icd.getStaticFunctionIds()) {
            InterpretedFunction staticFunction =
                    InterpretedFunction.createFunction(cx, scope, parent, staticFunctionId);
            String funName = staticFunction.getFunctionName();
            assert funName != null && !funName.isEmpty();
            nc.put(funName, nc, staticFunction); // Statics go on the class itself
        }

        // Store in scope
        String functionName = constructor.getFunctionName();
        if (functionName != null && !functionName.isEmpty()) {
            scope.put(functionName, scope, nc);
        }

        return nc;
    }

    private static Scriptable getPrototypePropertyAsScriptable(InterpretedFunction constructor) {
        Object prototypeProperty = constructor.getPrototypeProperty();
        if (!(prototypeProperty instanceof Scriptable)) {
            throw Kit.codeBug();
        }
        return (Scriptable) prototypeProperty;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        throw ScriptRuntime.typeErrorById("msg.class.constructor.needs.new", getFunctionName());
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        return constructor.construct(cx, scope, args);
    }

    @Override
    public String getFunctionName() {
        // TODO: does this allow modification? Should it?
        return constructor.getFunctionName();
    }

    // TODO: does the spec say anything about this?
    //	@Override
    //	public String getClassName() {
    //		throw new UnsupportedOperationException("TODO");
    //	}
}
