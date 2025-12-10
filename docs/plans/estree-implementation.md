# ESTree Implementation Plan

## Overview

This document outlines the plan to implement an ESTree-compliant AST representation for Rhino, alongside an adapter layer to convert from Rhino's existing AST nodes to ESTree format.

**Scope:** ESTree hierarchy implementation and adapter layer only. Parser and IRFactory modifications are explicitly **out of scope** for this phase.

---

## Goals

1. **ESTree Compliance**: Implement a complete ESTree-compliant AST hierarchy matching the latest ECMAScript standard
2. **Non-Breaking**: Maintain full backward compatibility with existing Rhino AST
3. **Modern Java**: Leverage Java 17 features (records, sealed types, pattern matching)

---

## Technical Foundation

### Language & Build

- **Java Version**: Java 17 (minimum)
- **Key Features Used**:
  - Records for immutable data classes
  - Sealed interfaces/classes for exhaustive type hierarchies
  - Pattern matching for type discrimination
  - Text blocks for documentation

### Package Structure

```
org.mozilla.javascript.estree/
├── nodes/              # ESTree node interfaces and implementations
│   ├── base/          # Base interfaces (Node, Expression, Statement, etc.)
│   ├── expressions/   # Expression node types
│   ├── statements/    # Statement node types
│   ├── declarations/  # Declaration node types
│   ├── literals/      # Literal node types
│   └── patterns/      # Destructuring patterns
├── types/             # Supporting types (Position, SourceLocation, Comment)
└── adapter/           # AstNode → ESTree conversion
```

---

## Position Strategy: Maximal Approach

Following industry practice (Babel, TypeScript-ESLint), we include all common position properties for maximum tool compatibility.

### Position Types

```java
package org.mozilla.javascript.estree.types;

/**
 * Position in source code. Line is 1-indexed, column is 0-indexed per ESTree spec.
 */
public record Position(
    int line,    // >= 1 (first line is 1)
    int column   // >= 0 (first character is 0)
) {
    public Position {
        if (line < 1) throw new IllegalArgumentException("line must be >= 1");
        if (column < 0) throw new IllegalArgumentException("column must be >= 0");
    }
}

/**
 * Source location with start and end positions.
 * Start is inclusive, end is exclusive.
 */
public record SourceLocation(
    Position start,
    Position end,
    String source  // usually null, can be filename
) {}
```

### Node Position Properties

Every ESTree node includes:

```java
public interface ESTreeNode {
    // Standard ESTree
    SourceLocation loc();

    // Common extensions for convenience and tool compatibility
    int start();      // absolute byte offset
    int end();        // absolute byte offset
    int[] range();    // [start, end] - for legacy compatibility
}
```

**Rationale:**
- `loc` satisfies ESTree specification
- `start`/`end` are convenient for consumers (no Position object allocation)
- `range` is expected by some tools (ESLint, older parsers)
- All three are redundant but widely supported

---

## Comment Strategy: Babel Approach

Comments are attached directly to nodes using Babel's three-category system.

### Comment Types

```java
package org.mozilla.javascript.estree.types;

/**
 * Base interface for comments. Sealed to Line and Block variants.
 */
public sealed interface Comment permits CommentLine, CommentBlock {
    String type();        // "CommentLine" or "CommentBlock"
    String value();       // comment text without delimiters
    int start();
    int end();
    SourceLocation loc();
}

/**
 * Single-line comment: // text
 */
public record CommentLine(
    String value,
    int start,
    int end,
    SourceLocation loc
) implements Comment {
    @Override
    public String type() { return "CommentLine"; }
}

/**
 * Multi-line comment: /* text *\/
 */
public record CommentBlock(
    String value,
    int start,
    int end,
    SourceLocation loc
) implements Comment {
    @Override
    public String type() { return "CommentBlock"; }
}
```

### Comment Attachment

Each node has three optional comment lists:

```java
public interface ESTreeNode {
    // Comment arrays (empty list if no comments, never null)
    List<Comment> leadingComments();   // comments before this node
    List<Comment> trailingComments();  // comments after node on same line
    List<Comment> innerComments();     // comments inside node (for blocks)
}
```

**Examples:**

```javascript
/* leading */
var x = 1; // trailing

function foo() {
  /* inner comment */
}
```

**Attachment Algorithm:**

Based on Babel's approach:
1. Comments before a node → `leadingComments`
2. Comments after a node on the same line → `trailingComments`
3. Comments inside a block/container → `innerComments`
4. Dangling comments attach to nearest enclosing node

---

## ESTree Node Hierarchy

### Base Interfaces

```java
package org.mozilla.javascript.estree.nodes.base;

/**
 * Root of ESTree node hierarchy. All nodes implement this.
 */
public sealed interface Node permits Expression, Statement, Declaration,
                                      Pattern, Literal, Comment, Program, ... {
    String type();
    SourceLocation loc();
    int start();
    int end();
    int[] range();

    List<Comment> leadingComments();
    List<Comment> trailingComments();
    List<Comment> innerComments();
}

/**
 * Base for all expressions.
 */
public sealed interface Expression extends Node
    permits Identifier, Literal, BinaryExpression, CallExpression, ... {}

/**
 * Base for all statements.
 */
public sealed interface Statement extends Node
    permits BlockStatement, IfStatement, ForStatement, ... {}

/**
 * Base for all declarations.
 */
public sealed interface Declaration extends Statement
    permits FunctionDeclaration, VariableDeclaration, ClassDeclaration, ... {}

/**
 * Base for destructuring patterns.
 */
public sealed interface Pattern extends Node
    permits Identifier, ArrayPattern, ObjectPattern, ... {}
```

### Node Categories

Following ESTree spec, nodes are categorized as:

1. **Program** - Root node
2. **Statements** - Control flow, declarations, blocks
3. **Expressions** - Values and operations
4. **Patterns** - Destructuring targets
5. **Literals** - Primitive values
6. **Declarations** - Function, variable, class declarations
7. **Clauses** - Switch cases, catch clauses

---

## Implementation Approach: Records

All concrete nodes are implemented as Java records for immutability and conciseness.

### Example: Binary Expression

```java
package org.mozilla.javascript.estree.nodes.expressions;

/**
 * Binary operation: left operator right
 * Examples: x + y, a * b, foo && bar
 */
public record BinaryExpression(
    // Position
    SourceLocation loc,
    int start,
    int end,

    // Comments
    List<Comment> leadingComments,
    List<Comment> trailingComments,
    List<Comment> innerComments,

    // Properties
    String operator,        // "+", "-", "*", "&&", etc.
    Expression left,
    Expression right
) implements Expression {

    @Override
    public String type() { return "BinaryExpression"; }

    @Override
    public int[] range() { return new int[]{start, end}; }

    // Canonical constructor with validation
    public BinaryExpression {
        if (operator == null) throw new IllegalArgumentException("operator required");
        if (left == null) throw new IllegalArgumentException("left required");
        if (right == null) throw new IllegalArgumentException("right required");

        // Defensive copies for mutable lists
        leadingComments = List.copyOf(leadingComments);
        trailingComments = List.copyOf(trailingComments);
        innerComments = List.copyOf(innerComments);
    }

    // Convenience constructor without comments
    public BinaryExpression(
        SourceLocation loc,
        int start,
        int end,
        String operator,
        Expression left,
        Expression right
    ) {
        this(loc, start, end, List.of(), List.of(), List.of(), operator, left, right);
    }
}
```

---

## Complete Node Type List

### ES5 Baseline

**Program:**
- `Program` - Root node containing body statements

**Statements:**
- `ExpressionStatement` - Expression as statement
- `BlockStatement` - `{ }` block
- `EmptyStatement` - `;`
- `DebuggerStatement` - `debugger;`
- `WithStatement` - `with (obj) { }`
- `ReturnStatement` - `return expr;`
- `LabeledStatement` - `label: statement`
- `BreakStatement` - `break [label];`
- `ContinueStatement` - `continue [label];`
- `IfStatement` - `if (test) consequent else alternate`
- `SwitchStatement` - `switch (discriminant) { cases }`
- `ThrowStatement` - `throw argument;`
- `TryStatement` - `try { } catch { } finally { }`
- `WhileStatement` - `while (test) body`
- `DoWhileStatement` - `do body while (test);`
- `ForStatement` - `for (init; test; update) body`
- `ForInStatement` - `for (left in right) body`

**Declarations:**
- `FunctionDeclaration` - `function id(params) { body }`
- `VariableDeclaration` - `var/let/const declarations`
- `VariableDeclarator` - Individual variable in declaration

**Expressions:**
- `Identifier` - Variable reference (also used for `undefined`, `Infinity`, `NaN`)
- `Literal` - Primitive value (string, number, boolean, null, regexp)
- `ThisExpression` - `this`
- `ArrayExpression` - `[elements]`
- `ObjectExpression` - `{properties}`
- `FunctionExpression` - `function (params) { body }`
- `UnaryExpression` - `+x`, `-x`, `!x`, `typeof x`, etc.
- `UpdateExpression` - `x++`, `++x`, `x--`, `--x`
- `BinaryExpression` - `x + y`, `x && y`, etc.
- `AssignmentExpression` - `x = y`, `x += y`, etc.
- `LogicalExpression` - `x && y`, `x || y`
- `MemberExpression` - `obj.prop`, `obj[expr]`
- `ConditionalExpression` - `test ? consequent : alternate`
- `CallExpression` - `callee(arguments)`
- `NewExpression` - `new callee(arguments)`
- `SequenceExpression` - `expr1, expr2, expr3`

**Note on Literals vs Identifiers:**
- **Literals** (keyword tokens): `null`, `true`, `false`, numeric/string/regexp literals
- **NOT Literals** (identifier references): `undefined`, `Infinity`, `NaN` - these are represented as `Identifier` nodes because they reference global variables that can be shadowed, not language keywords

**Patterns:**
- `Identifier` - Also used as pattern

**Clauses:**
- `SwitchCase` - `case test: consequent` or `default: consequent`
- `CatchClause` - `catch (param) { body }`

**Properties:**
- `Property` - Key-value pair in object literal

### ES6+ Extensions

**Statements:**
- `ForOfStatement` - `for (left of right) body`

**Declarations:**
- `ClassDeclaration` - `class id extends superClass { body }`

**Expressions:**
- `ArrowFunctionExpression` - `(params) => body`
- `YieldExpression` - `yield [argument]`
- `AwaitExpression` - `await argument`
- `ClassExpression` - `class [id] extends superClass { body }`
- `MetaProperty` - `new.target`, `import.meta`
- `TemplateLiteral` - `` `template ${expr}` ``
- `TaggedTemplateExpression` - `tag`template` `
- `SpreadElement` - `...expr` in array/call
- `ChainExpression` - Optional chaining `obj?.prop`

**Patterns:**
- `ArrayPattern` - `[a, b, c] = array`
- `ObjectPattern` - `{a, b, c} = object`
- `RestElement` - `...rest` in destructuring
- `AssignmentPattern` - `x = default` in destructuring

**Classes:**
- `ClassBody` - Container for class methods/properties
- `MethodDefinition` - Class method
- `PropertyDefinition` - Class field

**Modules:**
- `ImportDeclaration` - `import x from 'module'`
- `ExportNamedDeclaration` - `export { x }`
- `ExportDefaultDeclaration` - `export default expr`
- `ExportAllDeclaration` - `export * from 'module'`
- `ImportSpecifier` - Import binding
- `ExportSpecifier` - Export binding
- `ImportDefaultSpecifier` - Default import
- `ImportNamespaceSpecifier` - `* as namespace`

**Recent Additions:**
- `BigIntLiteral` - `123n`
- `ImportExpression` - Dynamic `import()`
- `PrivateIdentifier` - `#privateMember`
- `StaticBlock` - `static { }` in classes

---

## Adapter Layer

The adapter converts Rhino's `AstNode` tree to ESTree format.

### Architecture

```
┌─────────────┐
│  AstNode    │  Rhino's existing AST
│  (relative  │  - Relative positions
│   positions)│  - Mutable nodes
└──────┬──────┘  - Comment tracking
       │
       │ Convert
       ▼
┌─────────────┐
│ ESTreeNode  │  ESTree output
│ (absolute   │  - Absolute positions
│  positions) │  - Immutable records
└─────────────┘  - Attached comments
```

### Core Adapter Class

```java
package org.mozilla.javascript.estree.adapter;

/**
 * Converts Rhino AstNode trees to ESTree format.
 */
public class AstToESTreeAdapter {

    /**
     * Convert entire AST starting from root.
     */
    public Program convert(AstRoot root) {
        // ...
    }

    /**
     * Convert any AstNode to appropriate ESTree node.
     */
    public Node convert(AstNode node) {
        // ...
    }
}
```

### Position Calculation

**Challenge:** Rhino uses relative positions, ESTree uses absolute.

**Solution:**
1. Use `AstNode.getAbsolutePosition()` for start offset
2. Add `getLength()` to get end offset
3. Convert offsets to line/column by scanning source

**Optimization:** Consider caching line offset calculations for performance.

### Comment Attachment Algorithm

Based on Babel's approach, attach comments to nodes based on position.

---

## API Design

### Public Entry Point

```java
package org.mozilla.javascript.estree;

/**
 * Main entry point for ESTree conversion.
 */
public class ESTree {

    /**
     * Convert Rhino AST to ESTree format.
     */
    public static Program from(AstRoot root, String sourceCode) {
        AstToESTreeAdapter adapter = new AstToESTreeAdapter(sourceCode);
        return adapter.convert(root);
    }
}
```

---

## Testing Strategy

### Unit Tests

Test each node type conversion individually:

```java
@Test
void testBinaryExpressionConversion() {
    String code = "x + y";
    AstRoot rhino = parse(code);
    Program estree = ESTree.from(rhino, code);

    ExpressionStatement stmt = (ExpressionStatement) estree.body().get(0);
    BinaryExpression expr = (BinaryExpression) stmt.expression();

    assertEquals("BinaryExpression", expr.type());
    assertEquals("+", expr.operator());
    assertEquals("x", ((Identifier) expr.left()).name());
    assertEquals("y", ((Identifier) expr.right()).name());
}
```

### Position Tests

Verify position calculations are correct:

```java
@Test
void testPositionCalculation() {
    String code = "function foo() {\n  return 42;\n}";
    Program estree = ESTree.parse(code, "test.js", 1);

    FunctionDeclaration func = (FunctionDeclaration) estree.body().get(0);

    // Check absolute offsets
    assertEquals(0, func.start());
    assertEquals(code.length(), func.end());

    // Check line/column
    assertEquals(1, func.loc().start().line());
    assertEquals(0, func.loc().start().column());
    assertEquals(3, func.loc().end().line());
    assertEquals(1, func.loc().end().column());
}
```

### Comment Tests

Verify comment attachment:

```java
@Test
void testCommentAttachment() {
    String code = """
        /* leading */
        function foo() { // trailing
          return 42;
        }
        """;

    Program estree = ESTree.parse(code, "test.js", 1);
    FunctionDeclaration func = (FunctionDeclaration) estree.body().get(0);

    assertEquals(1, func.leadingComments().size());
    assertEquals("CommentBlock", func.leadingComments().get(0).type());
    assertEquals(" leading ", func.leadingComments().get(0).value());

    assertEquals(1, func.trailingComments().size());
    assertEquals("CommentLine", func.trailingComments().get(0).type());
    assertEquals(" trailing", func.trailingComments().get(0).value());
}
```

### Integration Tests

Test with real-world JavaScript:

```java
@Test
void testRealWorldCode() {
    String code = Files.readString(Path.of("test/fixtures/react-component.js"));
    Program estree = ESTree.parse(code, "react-component.js", 1);

    assertNotNull(estree);
    assertTrue(estree.body().size() > 0);

    // Validate with external tool (e.g., serialize to JSON and validate)
}
```

### Validation Against Existing Parsers

Compare output with established parsers:

```java
@Test
void testCompatibilityWithBabel() {
    String code = "const x = 1;";
    Program rhinoESTree = ESTree.parse(code, "test.js", 1);

    // Parse with Babel via Node.js script
    String babelJSON = runBabelParser(code);

    // Compare structure (allowing for minor differences)
    assertESTreeEquivalent(rhinoESTree, babelJSON);
}
```

---

## Out of Scope

The following are explicitly **not included** in this phase:

### Parser Modifications

- **No changes to Parser.java**: The existing Rhino parser continues to generate AstNode trees
- Parser will not directly generate ESTree nodes
- All ESTree generation happens via the adapter layer

### IRFactory Modifications

- **No changes to IRFactory.java**: Code generation continues to work from AstNode
- IRFactory transformation logic remains unchanged
- ESTree is an output format, not used in code generation pipeline

### CodeGenerator Changes

- No bytecode generation changes
- Existing compilation pipeline unaffected

### Rhino Extensions

The following Rhino-specific features are not mapped to ESTree:

- **XML Literals**: `var x = <tag>content</tag>`
- **E4X expressions**: XML DOM extensions
- **Legacy let expressions**: `let (x = 1) x + 2`
- **Array comprehensions**: `[for (x of arr) x * 2]`
- **Generator expressions**: `(for (x of arr) x * 2)`
- **Extension keywords**: Non-standard syntax

These will either:
- Throw `UnsupportedOperationException` in the adapter
- Be mapped to generic `Node` with raw properties
- Be documented as unsupported

---

## Implementation Phases

### Phase 1: Foundation (Core Types) ✅ COMPLETED

**Deliverables:**
- ✅ Position and SourceLocation records
- ✅ Comment types (CommentLine, CommentBlock)
- ✅ Base Node interface hierarchy
- ✅ Program node implementation

**Validation:**
- ✅ Unit tests for Position/SourceLocation
- ✅ Comment type tests
- ✅ Basic Program node creation

### Phase 2: ES5 Baseline Nodes ✅ COMPLETED

**Deliverables:**
- ✅ All ES5 statement nodes (If, For, While, etc.) - 17 nodes
- ✅ All ES5 expression nodes (Binary, Call, Member, etc.) - 14 nodes
- ✅ All ES5 declaration nodes (Function, Variable) - 3 nodes
- ✅ Literal nodes (SimpleLiteral, RegExpLiteral)
- ✅ Clause nodes (SwitchCase, CatchClause)
- ✅ Property node for object literals

**Implementation Notes:**
- **Minimal code**: Avoid code that isn't immediately necessary; we'll implement it later.
- **No convenience constructors**: All node types have only the canonical constructor with all parameters including comment lists. Convenience constructors were removed as nodes will primarily be created by the adapter/parser, not by hand. Special case of the previous point.
- **Immutable records**: All nodes are Java records with defensive copying of mutable collections
- **Validation**: Canonical constructors validate required fields and enum values

**Validation:**
- ✅ Unit test for each node type
- ✅ All tests pass
- ⏭️ Conversion tests for simple ES5 code (Phase 3)
- ⏭️ Position calculation tests (Phase 3)

### Phase 2.5: Literal Type Refactoring (Optional Enhancement)

**Status:** Proposed - Not yet implemented

**Rationale:**
While ESTree spec uses a single `Literal` type with an `Object value` field, Java's type system benefits from specialized literal types for type safety and pattern matching.

**Deliverables:**
- Convert `SimpleLiteral` from a single record to a sealed interface
- Create specialized literal subtypes:
  - `StringLiteral` - with `String value()` accessor
  - `NumberLiteral` - with `double value()` accessor
  - `BooleanLiteral` - with `boolean value()` accessor
  - `NullLiteral` - with no value field (represents null)
- Update `Literal` sealed interface permits clause
- Update tests to use specialized types

**Validation:**
- All existing tests pass with new types
- Add pattern matching examples
- Verify JSON serialization still outputs "Literal" as type

### Phase 2.6: Enum for operators

**Deliverables:**
- Enum classes for unary, binary, assignment operators
- Replaced raw strings in `UnaryExpression` etc with the new enums

### Phase 3: Adapter Layer (ES5)

**Deliverables:**
- AstToESTreeAdapter core class
- Position conversion (relative → absolute)
- Line/column calculation with caching
- Conversion methods for all ES5 nodes

**Validation:**
- Integration tests with real ES5 code
- Position accuracy tests
- Round-trip tests (parse → convert → serialize → parse again)

### Phase 4: Comment Attachment

**Deliverables:**
- CommentAttacher implementation
- Babel-style attachment algorithm
- Leading/trailing/inner comment logic
- Edge case handling (dangling comments, etc.)

**Validation:**
- Comment attachment tests for various scenarios
- Inline comment tests
- Block comment tests
- Multi-line comment tests

### Phase 5: ES6+ Extensions

**Deliverables:**
- Arrow functions, async/await
- Classes and class members
- Destructuring patterns (Array, Object)
- Template literals
- Spread/rest operators
- For-of loops

**Validation:**
- ES6 feature tests
- Modern JavaScript examples
- Compatibility tests with Babel/TypeScript

### Phase 6: ES2015+ Recent Features

**Deliverables:**
- BigInt literals
- Optional chaining (`?.`)
- Nullish coalescing (`??`)
- Private class members (`#field`)
- Static blocks
- Module import/export

**Validation:**
- Latest ECMAScript spec tests
- Edge case handling
- Feature detection tests

### Phase 7: Public API & Documentation

**Deliverables:**
- ESTree public facade class
- Comprehensive JavaDoc

**Validation:**
- API usability tests
- Documentation review
- Example code verification

### Phase 8: Integration & Polish

**Deliverables:**
- Performance optimization
- Error handling improvements
- Diagnostic messages
- Update module-info.java

**Validation:**
- Performance benchmarks
- Memory usage profiling
- Error message quality

---

## Future Enhancements (Post-Implementation)

After the core implementation is complete, these could be added:

1. **Visitor pattern**: Traversal helpers
2. **Builder fluent API**: Ergonomic node construction
3. **JSON schema validation**: Automatic validation against spec
4. **Direct parser**: ESTree-native parser (major undertaking)
5. **Performance mode**: Skip comments/locations for speed

---

## References

### ESTree Specification
- [ESTree Spec Repository](https://github.com/estree/estree)
- [ES5 Spec](https://github.com/estree/estree/blob/master/es5.md)
- [ES2015+ Extensions](https://github.com/estree/estree/blob/master/es2015.md)

### Tool Implementations
- [Babel Parser](https://github.com/babel/babel/tree/main/packages/babel-parser)
- [Acorn Parser](https://github.com/acornjs/acorn)
- [TypeScript-ESLint](https://typescript-eslint.io/packages/typescript-estree/)
- [Esprima](https://esprima.org/)

### Comment Attachment
- [Babel Comment Attachment](https://github.com/babel/babel/blob/main/packages/babel-parser/ast/comment-attachment.md)
- [estree-util-attach-comments](https://github.com/syntax-tree/estree-util-attach-comments)

---
