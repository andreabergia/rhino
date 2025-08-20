package org.mozilla.javascript;

public class NativeClass extends BaseFunction {
    static final byte CLASS_PROP_GETTER = 1 << 1;
    static final byte CLASS_PROP_SETTER = 1 << 2;
    static final byte CLASS_PROP_STATIC = 1 << 3;

    private final NativeFunction constructor;

    public NativeClass(NativeFunction constructor) {
        this.constructor = constructor;
    }

    static NativeClass createClass(
            Context cx,
            Scriptable scope,
            InterpretedFunction parent,
            Object baseClass,
            int constructorIndex,
            boolean putInScope) {
        InterpretedFunction constructor =
                InterpretedFunction.createFunction(cx, scope, parent, constructorIndex);
        return createClass(scope, constructor, baseClass, putInScope);
    }

    static NativeClass createClass(
            Scriptable scope, NativeFunction constructor, Object baseClass, boolean putInScope) {

        // Create the class and handle [[prototype]], prototype property, and constructor
        NativeClass nc = new NativeClass(constructor);

        ScriptableObject prototypeProperty = getPrototypePropertyAsScriptableObject(constructor);
        nc.setPrototypeProperty(prototypeProperty);
        prototypeProperty.put("constructor", prototypeProperty, nc);

        if (Undefined.isUndefined(baseClass)) {
            ScriptRuntime.setBuiltinProtoAndParent(nc, scope, TopLevel.Builtins.Function);
        } else {
            // Set prototype of the class and of the prototype property
            if (!(baseClass instanceof BaseFunction)) {
                throw ScriptRuntime.notFunctionError(baseClass);
            }
            nc.setParentScope(scope);
            nc.setPrototype((Scriptable) baseClass);

            Object baseClassPrototypeProperty = ((BaseFunction) baseClass).getPrototypeProperty();
            if (!(baseClassPrototypeProperty instanceof Scriptable)) {
                throw Kit.codeBug();
            }
            prototypeProperty.setPrototype((Scriptable) baseClassPrototypeProperty);
        }

        if (putInScope) {
            // Store class object in scope
            String functionName = constructor.getFunctionName();
            if (functionName != null && !functionName.isEmpty()) {
                scope.put(functionName, scope, nc);
            }
        }

        return nc;
    }

    private static ScriptableObject getPrototypePropertyAsScriptableObject(
            NativeFunction constructor) {
        Object prototypeProperty = constructor.getPrototypeProperty();
        if (!(prototypeProperty instanceof ScriptableObject)) {
            throw Kit.codeBug();
        }
        return (ScriptableObject) prototypeProperty;
    }

    public void defineClassProperty(
            Context cx, Scriptable scope, Object key, Object value, byte mask) {
        ScriptableObject target = this;
        if ((mask & CLASS_PROP_STATIC) == 0) {
            // Non-static properties go to the prototype property
            target = (ScriptableObject) this.getPrototypeProperty();
        }

        if ((mask & CLASS_PROP_GETTER) != 0 || (mask & CLASS_PROP_SETTER) != 0) {
            // TODO: can we implement some better API in ScriptableObject?
            ScriptableObject d = (ScriptableObject) cx.newObject(scope);
            d.put("name", d, key);
            d.put("configurable", d, true);
            d.put("enumerable", d, false);
            if ((mask & CLASS_PROP_GETTER) != 0) {
                d.put("get", d, value);
            }
            if ((mask & CLASS_PROP_SETTER) != 0) {
                d.put("set", d, value);
            }
            target.defineOwnProperty(cx, key, d);
        } else {
            if (ScriptRuntime.isSymbol(key)) {
                target.defineProperty((Symbol) key, value, 0);
            } else {
                target.defineProperty(ScriptRuntime.toString(key), value, 0);
            }
        }
    }

    InterpretedFunction createNestedFunction(Context cx, Scriptable scope, int index) {
        assert this.constructor instanceof InterpretedFunction;
        InterpretedFunction fun =
                InterpretedFunction.createFunction(
                        cx, scope, (InterpretedFunction) this.constructor, index);
        // TODO: arrow?
        return fun;
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
}
