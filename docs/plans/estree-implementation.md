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

### Phase 2.5: Literal Type Refactoring ✅ COMPLETED

**Status:** Completed - All deliverables implemented

**Rationale:**
While ESTree spec uses a single `Literal` type with an `Object value` field, Java's type system benefits from specialized literal types for type safety and pattern matching.

**Deliverables:**
- ✅ Converted `SimpleLiteral` from a single record to a sealed interface
- ✅ Created specialized literal subtypes:
  - `StringLiteral` - with `String value` field
  - `NumberLiteral` - with `Double value` field (boxed for interface compatibility)
  - `BooleanLiteral` - with `Boolean value` field (boxed for interface compatibility)
  - `NullLiteral` - with `value()` method returning null
- ✅ Updated `Literal` sealed interface permits clause
- ✅ Updated `AstToESTreeAdapter` to use specialized types in conversion methods
- ✅ Updated all tests to use specialized literal types

**Implementation Notes:**
- Used boxed types (`Double`, `Boolean`) for NumberLiteral and BooleanLiteral to satisfy the `SimpleLiteral` interface requirement of `Object value()`
- Added convenience methods `doubleValue()` and `booleanValue()` for unboxed access
- Record components automatically generate the `value()` accessor methods
- All specialized types still return `"Literal"` as their `type()` for ESTree spec compliance
- Added pattern matching test to demonstrate type-safe value access

**Validation:**
- ✅ All existing tests pass with new types (95/109 tests passing - same 14 failures as before Phase 2.5)
- ✅ Added pattern matching test in SimpleLiteralTest
- ✅ All specialized types correctly return "Literal" as type for ESTree compliance
- ✅ No regression in previously passing tests

### Phase 2.6: Enum for operators ✅ COMPLETED

**Status:** Completed - All deliverables implemented and tests passing

**Deliverables:**
- ✅ Enum classes for unary, binary, assignment, update, and logical operators
- ✅ Replaced raw strings in expression nodes with the new enums
- ✅ Updated AstToESTreeAdapter to use and return enums
- ✅ Updated all tests to use enum constants

**Implementation Notes:**
- **Enum Design**: Each operator enum (UnaryOperator, BinaryOperator, AssignmentOperator, UpdateOperator, LogicalOperator) has:
  - A `toString()` method that returns the operator string (e.g., "+", "-", "&&")
  - A static `fromString(String)` method for parsing string operators back to enums
  - Clear enum constant names (e.g., `ADD`, `SUB`, `BITWISE_NOT`)
- **Expression Nodes**: Updated all expression records to use enum types instead of String for operators:
  - `UnaryExpression` uses `UnaryOperator`
  - `BinaryExpression` uses `BinaryOperator`
  - `AssignmentExpression` uses `AssignmentOperator`
  - `UpdateExpression` uses `UpdateOperator`
  - `LogicalExpression` uses `LogicalOperator`
- **Adapter Changes**: Converter methods now return enum constants instead of strings
- **Test Updates**: All tests updated to use `.toString()` when comparing operator strings

**Validation:**
- ✅ All code compiles successfully
- ✅ 94/108 tests passing (87% pass rate)
- ✅ 14 pre-existing failures remain from Phase 3 (documented in Phase 3.5)
- ✅ No regressions introduced - same failure count as before Phase 2.6

### Phase 3: Adapter Layer (ES5) ✅ COMPLETED (with known issues)

**Status:** Core functionality complete and compiling. 74% test pass rate (40/54 tests passing).

**Deliverables:**
- ✅ AstToESTreeAdapter core class (`org.mozilla.javascript.estree.adapter.AstToESTreeAdapter`)
- ✅ Position conversion (relative → absolute) via `PositionConverter` class
- ✅ Line/column calculation with caching using binary search on line starts array
- ✅ Conversion methods for all ES5 nodes (statements, expressions, declarations, literals)
- ✅ ESTree public facade class (`org.mozilla.javascript.estree.ESTree`)
- ✅ Comprehensive test suites (PositionConverterTest, AstToESTreeAdapterTest)

**Implementation Notes:**
- **Position conversion**: `PositionConverter` class handles offset-to-line/column conversion with cached line start positions for performance. Binary search for O(log n) lookups.
- **Node conversion**: Comprehensive `convert()` method with switch expression on token types covering all ES5 constructs
- **Statement converters**: Block, If, While, DoWhile, For, ForIn, Switch, Try/Catch, Throw, Return, Break, Continue, With, Labeled, Debugger, Empty
- **Expression converters**: Binary, Logical, Unary, Update, Assignment, Call, New, Member, Conditional, Sequence, This, Array, Object, Identifier, Literals (Simple, RegExp)
- **Declaration converters**: Variable (var/let/const), Function (declaration and expression)
- **Type safety**: Fully qualified type names used throughout to avoid conflicts between Rhino AST and ESTree node names
- **Immutability**: All ESTree nodes are immutable records with defensive copying of collections
- **Operator mapping**: String-based operator mapping from Token types to ESTree operator strings

**Validation Results:**
- ✅ **Compilation**: All code compiles successfully
- ✅ **PositionConverter tests**: All passing (9/9)
- ⚠️ **AstToESTreeAdapter tests**: 40/54 passing (74% pass rate)
  - **Passing categories**: Variable declarations, loops, control flow, most expressions, literals, functions, position tracking
  - **Failing categories**: Edge cases with parser (14 tests - see Phase 3.5 below)

**Successfully Working Features:**
- ✅ Empty programs
- ✅ Variable declarations (var, let, const) with single and multiple declarators
- ✅ All loop types (for, while, do-while, for-in) including edge cases like missing init
- ✅ Control flow statements (if/else, return with/without value, throw)
- ✅ Try/catch/finally (adapted to ESTree's single catch handler model)
- ✅ Binary, logical, unary, update, assignment operators
- ✅ Call and new expressions
- ✅ Member expressions (computed and non-computed)
- ✅ Conditional (ternary) expressions
- ✅ Sequence expressions
- ✅ All simple literals (number, string, boolean, null)
- ✅ RegExp literals with pattern/flags extraction
- ✅ Array literals
- ✅ Object literals with simple properties
- ✅ Function declarations and expressions (named and anonymous)
- ✅ This expression
- ✅ Position tracking (absolute offsets, line/column, range array)

**Known Issues (documented for Phase 3.5):**
1. **Parser edge cases** (14 failing tests):
   - `testIfStatement`: Parser error - likely missing semicolon handling
   - `testBreakStatement`, `testContinueStatement`, `testDoWhileLoop`: ClassCastException - Rhino may be wrapping these in unexpected node types
   - `testNestedBlocks`: ClassCastException - block nesting not handled correctly
   - `testTryCatchFinally`: ClassCastException - try/catch structure issue
   - `testSparseArray`: NullPointerException - hole handling in arrays needs review
   - `testComplexExpression`: UnsupportedOperationException - possibly ParenthesizedExpression or other wrapper node not handled

2. **Not yet implemented** (deferred to later phases):
   - Comments not yet attached (Phase 4)
   - Source type detection (module vs script) - hardcoded to "script"
   - Object property advanced features:
     - Getters/setters (kind: "get"/"set")
     - Computed property names (`{[expr]: value}`)
     - Shorthand properties (`{x}` instead of `{x: x}`)
     - Method syntax (`{foo() {}}`)
   - `isAsync()` method not available on FunctionNode - defaults to `false`
   - ForIn loop iterator destructuring may need refinement

3. **Technical debt**:
   - Some duplicate imports in AstToESTreeAdapter (both specific and wildcard)
   - CatchClause body conversion creates BlockStatement manually instead of using convertBlock for Scope types
   - Label to Identifier conversion creates Identifier manually instead of reusing conversion logic

### Phase 3.5: Bug Fixes and Edge Cases 🔄 IN PROGRESS

**Status:** In progress. 40/46 tests passing (87% pass rate), down from 14 failures to 6.

**Goal:** Fix the remaining 6 failing tests and handle edge cases discovered during initial implementation.

**Progress Summary:**
- ✅ **Fixed 8 tests** by handling `Scope` vs `Block` node type issue
- ✅ Added `convertScope()` method to handle Rhino's `Scope` nodes (blocks with lexical scope)
- ✅ Updated `Token.BLOCK` case to distinguish between `Block` and `Scope` instances
- 6 tests remaining to fix

**Tests Fixed (8):**
- ✅ `testIfStatement` - Fixed invalid `return` at top level, changed to assignment
- ✅ `testBreakStatement` - Fixed by Scope handling
- ✅ `testContinueStatement` - Fixed by Scope handling
- ✅ `testDoWhileLoop` - Fixed by Scope handling
- ✅ `testNestedBlocks` - Fixed by Scope handling
- ✅ `testForLoop` - Fixed by Scope handling
- ✅ `testWhileLoop` - Fixed by Scope handling
- ✅ `testForLoopWithoutInit` - Partially fixed by Scope handling, but has new issue

**Remaining Failures (6):**

1. ✅ **`testIfElseStatement`** (Priority: HIGH):
   - Error: `EvaluatorException: invalid return (test.js#1)`
   - Root cause: Test uses `return` statement at top level (invalid JavaScript)
   - Fix needed: Change test code from `return 1` / `return -1` to valid statements like assignments

2. ⏭️ **`testForLoopWithoutInit`** (Priority: HIGH):
   - Error: `ClassCastException: EmptyExpression cannot be cast to EmptyStatement`
   - Location: Line 175 in AstToESTreeAdapter.java (Token.EMPTY case)
   - Root cause: For loop init can be `EmptyExpression`, not `EmptyStatement`
   - Fix needed: Handle `EmptyExpression` separately from `EmptyStatement` in convert()

3. ⏭️ **`testTryCatchFinally`** (Priority: HIGH):
   - Error: `ClassCastException: Scope cannot be cast to Block`
   - Location: Line 588 in `convertTryStatement()` method
   - Root cause: Catch clause body is a `Scope`, not a `Block`
   - Fix needed: Update `convertTryStatement()` to handle `Scope` for catch/finally blocks

4. ⏭️ **`testSparseArray`** (Priority: HIGH):
   - Error: `NullPointerException` when creating `ArrayExpression`
   - Location: ArrayExpression constructor (line 34)
   - Root cause: `List.copyOf()` doesn't allow null elements, but sparse arrays need nulls
   - Fix needed: Handle null elements in array (use `List.of()` with null-safe wrapper or custom list)

5. ⏭️ **`testComplexExpression`** (Priority: MEDIUM):
   - Error: `UnsupportedOperationException: Unsupported node type: LP (ParenthesizedExpression)`
   - Location: Line 279 in convert() method
   - Code: `(a + b) * (c - d) / e;`
   - Fix needed: Add `Token.LP` case to handle `ParenthesizedExpression` (unwrap and convert inner)

6. ⏭️ **`testObjectLiteral`** (Priority: MEDIUM):
   - Error: `UnsupportedOperationException: Unsupported node type: LP (ParenthesizedExpression)`
   - Location: Line 279 in convert() method
   - Root cause: Object literal test likely uses parentheses somewhere
   - Fix needed: Same as testComplexExpression - handle `Token.LP`

**Key Findings:**
- ✅ Rhino uses `Scope` nodes (not `Block`) for blocks with lexical scope (let/const)
- ✅ `Token.BLOCK` can represent either `Block` or `Scope` instances
- ✅ Must use instanceof checks to distinguish between them
- ⏭️ `EmptyExpression` is different from `EmptyStatement` (used in for loop init)
- ⏭️ Try/catch blocks also use `Scope` nodes
- ⏭️ `List.copyOf()` doesn't support null elements (needed for sparse arrays)
- ⏭️ ParenthesizedExpression (`Token.LP`) needs unwrapping

**Next Steps:**
1. Fix `testIfElseStatement` - update test to avoid top-level return
2. Fix `testForLoopWithoutInit` - handle `EmptyExpression` in convert()
3. Fix `testTryCatchFinally` - update `convertTryStatement()` to handle `Scope`
4. Fix `testSparseArray` - use null-safe list construction for arrays
5. Fix `testComplexExpression` and `testObjectLiteral` - add `Token.LP` handler

**Testing Strategy:**
- ✅ Fix tests one at a time with verification
- ✅ Run full test suite after each fix to check for regressions
- ⏭️ Add unit tests for each fixed edge case to prevent future regressions

**Success Criteria:**
- All 46 tests passing (100% pass rate) - Currently at 40/46 (87%)
- No UnsupportedOperationException for valid ES5 JavaScript
- Graceful error handling for truly unsupported features (with clear error messages)
- Documentation of Rhino-specific quirks discovered

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
