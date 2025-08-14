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

        ScriptableObject prototypeProperty = getPrototypePropertyAsScriptableObject(constructor);
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

        // Getter and setter properties
        for (InterpreterClassData.GetterSetterProperty prop : icd.getGetterSetterProperties()) {
            // TODO: we can probably use a better API, but defineOwnProperty feels too heavyweight
            //  and the alternatives that take Method or LambdaFunction cannot be used. I probably
            //  need to add a new overload in ScriptableObject, but for the moment this mimics
            //  ScriptRuntime::fillObjectLiteral

            // TODO: index names (i.e. property "get 0")

            if (prop.getGetterId() != -1) {
                InterpretedFunction getter =
                        InterpretedFunction.createFunction(cx, scope, parent, prop.getGetterId());
                prototypeProperty.setGetterOrSetter(prop.getName(), 0, getter, false);
            }
            if (prop.getSetterId() != -1) {
                InterpretedFunction setter;
                setter = InterpretedFunction.createFunction(cx, scope, parent, prop.getSetterId());
                prototypeProperty.setGetterOrSetter(prop.getName(), 0, setter, true);
            }
        }

        // Store class object in scope
        String functionName = constructor.getFunctionName();
        if (functionName != null && !functionName.isEmpty()) {
            scope.put(functionName, scope, nc);
        }

        return nc;
    }

    private static ScriptableObject getPrototypePropertyAsScriptableObject(
            InterpretedFunction constructor) {
        Object prototypeProperty = constructor.getPrototypeProperty();
        if (!(prototypeProperty instanceof ScriptableObject)) {
            throw Kit.codeBug();
        }
        return (ScriptableObject) prototypeProperty;
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
