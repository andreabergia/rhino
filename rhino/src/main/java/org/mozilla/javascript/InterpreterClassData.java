package org.mozilla.javascript;

import java.util.List;

final class InterpreterClassData {
    // TODO: what else? extends, properties, ...?
    private final int constructorFunctionId;

    // TODO: migrate to an int[] for reduced memory usage
    private final List<Integer> memberFunctionIds;
    private final List<Integer> staticFunctionIds;
    private final List<GetterSetterProperty> getterSetterProperties;

    InterpreterClassData(
            int constructorFunctionId,
            List<Integer> memberFunctionIds,
            List<Integer> staticFunctionIds,
            List<GetterSetterProperty> getterSetterProperties) {
        this.constructorFunctionId = constructorFunctionId;
        this.memberFunctionIds = memberFunctionIds;
        this.staticFunctionIds = staticFunctionIds;
        this.getterSetterProperties = getterSetterProperties;
    }

    public int getConstructorFunctionId() {
        return constructorFunctionId;
    }

    public List<Integer> getMemberFunctionIds() {
        return memberFunctionIds;
    }

    public List<Integer> getStaticFunctionIds() {
        return staticFunctionIds;
    }

    public List<GetterSetterProperty> getGetterSetterProperties() {
        return getterSetterProperties;
    }

    static final class GetterSetterProperty {
        private final String name;
        private final int getterId; // -1: not found
        private final int setterId; // -1: not present

        public GetterSetterProperty(String name, int getterId, int setterId) {
            assert getterId != -1 || setterId != -1;
            this.name = name;
            this.getterId = getterId;
            this.setterId = setterId;
        }

        public String getName() {
            return name;
        }

        public int getGetterId() {
            return getterId;
        }

        public int getSetterId() {
            return setterId;
        }
    }
}
