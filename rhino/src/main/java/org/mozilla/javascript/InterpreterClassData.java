package org.mozilla.javascript;

import java.util.List;

final class InterpreterClassData {
    // TODO: what else? extends, properties, ...?
    private final int constructorFunctionId;

    // TODO: migrate to an int[] for reduced memory usage
    private final List<Integer> memberFunctionIds;
    private final List<Integer> staticFunctionIds;
    private final List<AccessorProperty> accessorProperties;
    private final List<AccessorProperty> staticAccessorProperties;

    InterpreterClassData(int constructorFunctionId) {
        this.constructorFunctionId = constructorFunctionId;
        this.memberFunctionIds = List.of();
        this.staticFunctionIds = List.of();
        this.accessorProperties = List.of();
        this.staticAccessorProperties = List.of();
    }

    InterpreterClassData(
            int constructorFunctionId,
            List<Integer> memberFunctionIds,
            List<Integer> staticFunctionIds,
            List<AccessorProperty> accessorProperties,
            List<AccessorProperty> staticAccessorProperties) {
        this.constructorFunctionId = constructorFunctionId;
        this.memberFunctionIds = memberFunctionIds;
        this.staticFunctionIds = staticFunctionIds;
        this.accessorProperties = accessorProperties;
        this.staticAccessorProperties = staticAccessorProperties;
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

    public List<AccessorProperty> getAccessorProperties() {
        return accessorProperties;
    }

    public List<AccessorProperty> getStaticAccessorProperties() {
        return staticAccessorProperties;
    }

    @Override
    public String toString() {
        return "InterpreterClassData{"
                + "constructorFunctionId="
                + constructorFunctionId
                + ", memberFunctionIds="
                + memberFunctionIds
                + ", staticFunctionIds="
                + staticFunctionIds
                + ", accessorProperties="
                + accessorProperties
                + ", staticAccessorProperties="
                + staticAccessorProperties
                + '}';
    }

    static final class AccessorProperty {
        private final String name;
        private final int getterId; // -1: not found
        private final int setterId; // -1: not present

        public AccessorProperty(String name, int getterId, int setterId) {
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
