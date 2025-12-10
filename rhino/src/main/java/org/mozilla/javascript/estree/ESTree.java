package org.mozilla.javascript.estree;

import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.estree.adapter.AstToESTreeAdapter;
import org.mozilla.javascript.estree.nodes.Program;

/**
 * Main entry point for ESTree conversion.
 *
 * <p>This class provides a simple facade for converting Rhino's AST to ESTree format.
 *
 * @see AstToESTreeAdapter
 */
public final class ESTree {
    private ESTree() {}

    /**
     * Converts a Rhino AST to ESTree format.
     *
     * @param root the Rhino AST root node
     * @param sourceCode the complete source code text
     * @param sourceName the filename or URI of the source (optional, can be null)
     * @return the ESTree Program node
     */
    public static Program from(AstRoot root, String sourceCode, String sourceName) {
        AstToESTreeAdapter adapter = new AstToESTreeAdapter(sourceCode, sourceName);
        return adapter.convertProgram(root);
    }
}
