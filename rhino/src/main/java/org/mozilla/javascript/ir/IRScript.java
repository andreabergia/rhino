package org.mozilla.javascript.ir;

import java.util.List;

public record IRScript(List<IRInstruction> instructions) {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IRScript: \n");
        for (IRInstruction instruction : instructions) {
            sb.append("  ").append(instruction).append("\n");
        }
        return sb.toString();
    }
}
