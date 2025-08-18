package org.mozilla.javascript;

import java.util.List;
import org.mozilla.javascript.InterpreterClassData.AccessorProperty;

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
        createFunctions(cx, scope, constructor, icd.getMemberFunctionIds(), prototypeProperty);
        createFunctions(cx, scope, constructor, icd.getStaticFunctionIds(), nc);

        // Accessor (i.e. getter and setter) properties
        createAccessorProperties(
                cx, scope, constructor, icd.getAccessorProperties(), prototypeProperty);
        createAccessorProperties(cx, scope, constructor, icd.getStaticAccessorProperties(), nc);

        // Store class object in scope
        String functionName = constructor.getFunctionName();
        if (functionName != null && !functionName.isEmpty()) {
            scope.put(functionName, scope, nc);
        }

        return nc;
    }

    private static void createFunctions(
            Context cx,
            Scriptable scope,
            InterpretedFunction parent,
            List<Integer> functionIds,
            ScriptableObject owner) {
        for (Integer memberFunctionId : functionIds) {
            InterpretedFunction member =
                    InterpretedFunction.createFunction(cx, scope, parent, memberFunctionId);
            String memberName = member.getFunctionName();
            assert memberName != null && !memberName.isEmpty();
            owner.put(memberName, owner, member);
        }
    }

    private static void createAccessorProperties(
            Context cx,
            Scriptable scope,
            InterpretedFunction parent,
            List<AccessorProperty> accessorProperties,
            ScriptableObject owner) {
        for (AccessorProperty prop : accessorProperties) {
            // TODO: we probably want to create a new API that does not need the creation of the
            //  descriptor nor the wrapping in LambdaGetterFunction/LambdaSetterFunction
            // TODO: index names (i.e. property "get 0")

            LambdaGetterFunction getter = null;
            if (prop.getGetterId() != -1) {
                var underlyingGetter =
                        InterpretedFunction.createFunction(cx, scope, parent, prop.getGetterId());
                getter = thisObj -> underlyingGetter.call(cx, scope, thisObj, new Object[0]);
            }
            LambdaSetterFunction setter = null;
            if (prop.getSetterId() != -1) {
                var underlyingSetter =
                        InterpretedFunction.createFunction(cx, scope, parent, prop.getSetterId());
                setter =
                        (thisObj, value) ->
                                underlyingSetter.call(cx, scope, thisObj, new Object[] {value});
            }
            owner.defineProperty(cx, prop.getName(), getter, setter, DONTENUM);
        }
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
