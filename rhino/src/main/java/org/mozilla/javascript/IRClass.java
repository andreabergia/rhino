package org.mozilla.javascript;

final class IRClass {
    private final int classIndex;
    private final boolean isStatement;

    public IRClass(int classIndex, boolean isStatement) {
        this.classIndex = classIndex;
        this.isStatement = isStatement;
    }

    public int getClassIndex() {
        return classIndex;
    }

    public boolean isStatement() {
        return isStatement;
    }
}
