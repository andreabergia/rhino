package org.mozilla.javascript.estree.adapter;

import java.util.Arrays;
import org.mozilla.javascript.estree.types.Position;
import org.mozilla.javascript.estree.types.SourceLocation;

/**
 * Converts between character offsets and line/column positions.
 *
 * <p>ESTree uses 1-indexed lines and 0-indexed columns. This class handles the conversion from
 * absolute character offsets to Position objects, with caching for performance.
 *
 * <p>Thread-safe and immutable after construction.
 */
public class PositionConverter {

    private final String sourceCode;
    private final int[] lineStarts; // Cache of line start offsets
    private final String sourceName; // Optional filename

    /**
     * Creates a position converter for the given source code.
     *
     * @param sourceCode the complete source code text
     * @param sourceName optional filename or URI for the source
     */
    public PositionConverter(String sourceCode, String sourceName) {
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.lineStarts = computeLineStarts(sourceCode);
    }

    /** Creates a position converter with no source name. */
    public PositionConverter(String sourceCode) {
        this(sourceCode, null);
    }

    /**
     * @return array where lineStarts[i] is the offset of line i+1
     */
    private static int[] computeLineStarts(String sourceCode) {
        // Count newlines first to size array
        int lineCount = 1;
        for (int i = 0; i < sourceCode.length(); i++) {
            if (sourceCode.charAt(i) == '\n') {
                lineCount++;
            }
        }

        // Build array of line start positions
        int[] starts = new int[lineCount];
        starts[0] = 0; // First line starts at 0

        int lineIndex = 1;
        for (int i = 0; i < sourceCode.length(); i++) {
            if (sourceCode.charAt(i) == '\n') {
                starts[lineIndex++] = i + 1; // Next line starts after \n
            }
        }

        return starts;
    }

    /**
     * Converts an absolute character offset to a Position.
     *
     * @param offset the absolute character offset (0-indexed)
     * @return the Position with 1-indexed line and 0-indexed column
     * @throws IllegalArgumentException if offset is out of bounds
     */
    public Position offsetToPosition(int offset) {
        if (offset < 0 || offset > sourceCode.length()) {
            throw new IllegalArgumentException(
                    "Offset " + offset + " out of bounds [0, " + sourceCode.length() + "]");
        }

        // Binary search to find the line
        int lineIndex = Arrays.binarySearch(lineStarts, offset);

        if (lineIndex >= 0) {
            // Exact match - offset is at start of line
            return new Position(lineIndex + 1, 0);
        } else {
            // Not exact - convert insertion point to line index
            lineIndex = -lineIndex - 2; // Line containing offset
            int lineStart = lineStarts[lineIndex];
            int column = offset - lineStart;
            return new Position(lineIndex + 1, column);
        }
    }

    /**
     * Creates a SourceLocation from start and end offsets.
     *
     * @param start the start offset (inclusive)
     * @param end the end offset (exclusive)
     * @return the SourceLocation spanning from start to end
     */
    public SourceLocation createLocation(int start, int end) {
        Position startPos = offsetToPosition(start);
        Position endPos = offsetToPosition(end);
        return new SourceLocation(startPos, endPos, sourceName);
    }

    public String getSourceName() {
        return sourceName;
    }

    public int getLineCount() {
        return lineStarts.length;
    }
}
