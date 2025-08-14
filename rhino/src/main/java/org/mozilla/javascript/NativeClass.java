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
        // First step: create the constructor function
        InterpretedFunction constructor =
                InterpretedFunction.createFunction(
                        cx, scope, parent, icd.getConstructorFunctionId());

        // Then we create the class and handle the [[prototype]], prototype property, and
        // constructor property
        NativeClass nc = new NativeClass(constructor);

        // TODO: we need to handle extends. If nothing, this means we have the Function.prototype
        ScriptRuntime.setBuiltinProtoAndParent(nc, scope, TopLevel.Builtins.Function);

        Scriptable prototypeProperty = getPrototypePropertyAsScriptable(constructor);
        nc.setPrototypeProperty(prototypeProperty);
        prototypeProperty.put("constructor", prototypeProperty, nc);

        // Member and static functions
        for (Integer memberFunctionId : icd.getMemberFunctionIds()) {
            InterpretedFunction member =
                    InterpretedFunction.createFunction(cx, scope, parent, memberFunctionId);
            String memberName = member.getFunctionName();
            assert memberName != null && !memberName.isEmpty();

            // Members go to the prototype property
            prototypeProperty.put(memberName, prototypeProperty, member);
        }
        for (Integer staticFunctionId : icd.getStaticFunctionIds()) {
            InterpretedFunction staticFunction =
                    InterpretedFunction.createFunction(cx, scope, parent, staticFunctionId);
            String funName = staticFunction.getFunctionName();
            assert funName != null && !funName.isEmpty();
            // Statics go on the class itself
            nc.put(funName, nc, staticFunction);
        }

        // Store class object in scope
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
