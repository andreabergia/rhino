# Rhino Compiler Pipeline Rewrite

## Context

Rhino's current compiler pipeline suffers from deep structural problems:
- `AstNode extends Node` couples the public AST to the internal IR
- `Token.java` constants are shared across lexer, parser, IR, and interpreter opcodes
- The IR is a weakly-typed tree with gotos encoded as properties - hard to analyze or optimize
- Adding new ES features requires touching many tightly-coupled components
- The pipeline is ~15k+ lines of intertwined code with no clean layer boundaries

This plan replaces the entire compilation pipeline (lexer, parser, AST, IR, code generators) with a
modern, cleanly layered architecture using Java 17+ features (sealed interfaces, records, pattern
matching). The runtime (`ScriptRuntime`, `Interpreter`, `Scriptable`, builtins, etc.) is kept as-is.

The new architecture:

```
Source → New Lexer → New Parser → New AST (sealed interfaces/records)
                                       ↓
                                  IR Builder (desugaring + scope analysis)
                                       ↓
                                  CFG-based IR (basic blocks + registers)
                                       ↓
                       ┌───────────────┴───────────────┐
                  New CodeGenerator              New Codegen
                 (IR → Icode for existing       (IR → JVM bytecode)
                  Interpreter)
                       └───────────┬───────────────┘
                            ScriptRuntime (unchanged)
```

## Strategy: Incremental, Inside-Out

Start from the center (IR), work outward to backends, then to the frontend. At every step the
system remains fully functional and the existing test suite passes.

---

## Phase 0: Java 17 Upgrade ✅

**Goal:** Enable sealed interfaces, records, pattern matching for switch.

- Update `build.gradle` to set source/target compatibility to Java 17
- Update CI configuration
- Update `AGENTS.md` to reflect Java 17
- Verify full test suite passes

**Files:** `build.gradle`, CI configs, `AGENTS.md`

---

## Phase 1: Define the New IR ✅

**Goal:** Create the CFG-based IR data structures and utilities, with no connections to the existing
pipeline yet.

**Package:** `org.mozilla.javascript.compiler.ir`

**Detailed design:** see [`modernization-ir.md`](modernization-ir.md)

### IR Design

The IR uses **basic blocks in SSA form** (Static Single Assignment). Each function/script is a
`CfgFunction` containing `BasicBlock`s. Each block has a list of `Instruction`s (starting with Phi
nodes at join points) and one `Terminator`. Instructions produce values into `Register`s; each
register is defined exactly once (SSA property). An `isSsa` flag tracks whether a function has been
converted. The builder produces non-SSA IR; SSA construction (dominance computation, phi insertion,
renaming) is deferred to Phase 2.

All types use Java 17 sealed interfaces and records. Dispatch uses `instanceof` pattern matching
(if-else chains); `switch` pattern matching requires Java 21+.

Key types:

- **`CfgFunction`** - top-level container: name, params, blocks, entry block, nested functions,
  flags (strict, generator, arrow, etc.), `FunctionKind` enum, exception handler table, register
  count, `isSsa` flag
- **`BasicBlock`** - `record(BlockId id, List<Instruction> instructions, Terminator terminator)`
- **`Register`** - `record(int id)`, **`BlockId`** - `record(int id)`
- **`Instruction`** (sealed) - 55 record variants covering:
  - SSA: `Phi`, `Move`
  - Constants: `LoadConstant` (with `IrConstant` sealed interface), `LoadTemplateLiteral`
  - Variables: `GetVar`, `SetVar`, `SetConstVar`
  - Scope chain: `GetName`, `SetName`, `StrictSetName`, `SetConst`, `BindName`, `TypeOfName`,
    `DeleteName`
  - Properties: `GetProp`, `GetPropNoWarn`, `GetPropSuper`, `SetProp`, `SetPropSuper`, `GetElem`,
    `GetElemSuper`, `SetElem`, `SetElemSuper`, `DeleteProp`, `DeleteElem`, `DeletePropSuper`,
    `GetPropOptional`, `GetElemOptional`
  - Operators: `BinaryOp` (with `BinOp` enum), `UnaryOp` (with `UnOp` enum)
  - Inc/dec: `IncDecVar`, `IncDecName`, `IncDecProp`, `IncDecElem` (with `IncDecOp` enum)
  - Function resolution: `NameAndThis`, `PropAndThis`, `ElemAndThis`, `ValueAndThis`
  - Calls: `Call` (with `CallKind` enum), `New`
  - Literals: `NewArray`, `NewObject`, `InitProp`, `InitComputedProp`, `InitArrayElement`,
    `ArraySpread`, `ObjectSpread`, `ObjectRest`
  - Closures: `CreateClosure`
  - Scope: `EnterWith`, `LeaveWith`, `GetCaughtException`, `EnterCatch`
  - Enumeration: `EnumInit` (with `EnumKind` enum), `EnumNext`, `EnumId`
  - Special: `GetThis`, `GetThisFn`, `GetSuper`, `GetNewTarget`
  - Generators: `Yield`, `YieldStar`, `GeneratorStart`, `GeneratorReturn`, `GeneratorEnd`
  - Ref operations: `GetRef`, `SetRef`, `DeleteRef`, `RefSpecial`
- **`Terminator`** (sealed) - `Jump`, `CondJump`, `CondJumpNullUndef`, `Return`, `ReturnVoid`,
  `Throw`, `Rethrow`, `Switch` (with `SwitchCase`), `GoSub`
- **`ExceptionHandler`** - `record(List<BlockId> protectedBlocks, BlockId handlerBlock, boolean isFinally)`

### Utilities

- **`CfgAnalysis`** - computes predecessor/successor maps and reverse postorder from a `CfgFunction`
- **`CfgBuilder`** - fluent API for constructing `CfgFunction` (produces non-SSA IR)
- **`IrPrinter`** - human-readable textual dump of the CFG
- **`IrValidator`** - structural invariant checker with `NON_SSA` and `SSA` modes; recursively
  validates nested functions

### Testing

- Unit tests that construct IR via `CfgBuilder`, validate with `IrValidator`, dump with `IrPrinter`
- Hand-crafted SSA functions with phi nodes tested against SSA validation
- No integration with the rest of Rhino yet

**Files created:**
- `rhino/src/main/java/org/mozilla/javascript/compiler/ir/` — 19 source files: `Register`,
  `BlockId`, `BinOp`, `UnOp`, `IncDecOp`, `EnumKind`, `CallKind`, `LiteralPropertyKind`,
  `FunctionKind`, `IrConstant`, `Instruction`, `Terminator`, `BasicBlock`, `ExceptionHandler`,
  `CfgFunction`, `CfgAnalysis`, `CfgBuilder`, `IrPrinter`, `IrValidator`
- `rhino/src/test/java/org/mozilla/javascript/compiler/ir/` — `CfgBuilderTest`, `IrPrinterTest`,
  `IrValidatorTest`

---

## Phase 2: Old IR → New IR Adapter

**Goal:** Bridge the existing pipeline to the new IR so we can test backends against the full test suite.

**Package:** `org.mozilla.javascript.compiler.adapter`

Write a `NodeToCfg` converter that takes a `ScriptNode` (output of `IRFactory.transformTree`) and
produces a `CfgFunction`. This is temporary/throwaway code but gives us full test coverage immediately.

The converter must handle:
- Walking the Node tree's linked-list children
- Splitting at `TARGET` nodes to create basic block boundaries
- Converting `GOTO`/`IFEQ`/`IFNE` to `Jump`/`CondJump` terminators
- Converting expression subtrees to sequences of `Instruction`s with `Register` allocation
- Mapping `TRY`/`CATCH`/`FINALLY` structures to `ExceptionHandler` entries
- Processing nested `FunctionNode`s recursively
- Extracting metadata for `CfgFunction` flags

### Testing

- Run the full existing test suite: old parser → old IRFactory → `NodeToCfg` → validate with
  `IrValidator` → dump with `IrPrinter` for inspection
- Compare IR output against expected patterns for representative programs

**Files to create:**
- `rhino/src/main/java/org/mozilla/javascript/compiler/adapter/NodeToCfg.java`
- Tests in `rhino/src/test/java/org/mozilla/javascript/compiler/adapter/`

---

## Phase 3: New IR → Icode Backend

**Goal:** Generate Icode bytecode from the new IR, targeting the existing `Interpreter`.

**Package:** `org.mozilla.javascript.compiler.backend.interp`

Write `CfgToIcode` that takes a `CfgFunction` and produces `InterpreterData` (the existing bytecode
format consumed by `Interpreter.java`). This means:

- Linearize the CFG (order basic blocks for sequential execution)
- Convert registers to stack operations (register-to-stack lowering)
- Emit Icode/Token bytecodes for each instruction
- Generate string/number/bigint literal tables
- Generate exception handler tables
- Handle generator save/restore points

Also implement a new `Evaluator` (`NewInterpreterEvaluator`) that:
1. Receives a `ScriptNode` from `Context.parse()` (old parser still in use)
2. Runs `NodeToCfg` (Phase 2 adapter)
3. Runs `CfgToIcode`
4. Produces `JSDescriptor` with the generated `InterpreterData`

### Testing

- Wire `NewInterpreterEvaluator` into `Context` (selectable via optimization level or system property)
- Run the **entire** existing test suite through the new path
- Compare results with the old `CodeGenerator` path

**Key files to study:**
- `rhino/src/main/java/org/mozilla/javascript/CodeGenerator.java` (2,014 lines) - the current Icode
  generator; the new `CfgToIcode` replaces this
- `rhino/src/main/java/org/mozilla/javascript/InterpreterData.java` - the output format
- `rhino/src/main/java/org/mozilla/javascript/Interpreter.java` - the consumer

**Files to create:**
- `rhino/src/main/java/org/mozilla/javascript/compiler/backend/interp/CfgToIcode.java`
- `rhino/src/main/java/org/mozilla/javascript/compiler/backend/interp/NewInterpreterEvaluator.java`
- Tests

---

## Phase 4: New IR → JVM Backend

**Goal:** Generate JVM bytecode from the new IR.

**Package:** `org.mozilla.javascript.compiler.backend.jvm`

Write `CfgToJvm` that takes a `CfgFunction` and produces JVM class files. Consider using the ASM
library instead of the existing `ClassFileWriter` for better maintainability and features.

Generated classes must:
- Implement `JSCode<T>` interface (`execute` and `resume` methods)
- Call `ScriptRuntime` / `OptRuntime` static methods for all operations
- Handle generator state machines for `resume()`
- Produce `JSDescriptor` with all required metadata

Also implement `NewCompiledEvaluator` following the same pattern as Phase 3.

### Testing

- Run full test suite through new JVM backend
- Compare output with old `Codegen`/`BodyCodegen` path

**Key files to study:**
- `rhino/src/main/java/org/mozilla/javascript/optimizer/Codegen.java` (1,006 lines)
- `rhino/src/main/java/org/mozilla/javascript/optimizer/BodyCodegen.java` (4,912 lines)
- `rhino/src/main/java/org/mozilla/javascript/optimizer/OptRuntime.java`

**Files to create:**
- `rhino/src/main/java/org/mozilla/javascript/compiler/backend/jvm/CfgToJvm.java`
- `rhino/src/main/java/org/mozilla/javascript/compiler/backend/jvm/NewCompiledEvaluator.java`
- Tests

---

## Phase 5: New AST

**Goal:** Define clean AST types for the new parser to produce.

**Package:** `org.mozilla.javascript.compiler.ast`

Design principles:
- Sealed interfaces with records where appropriate
- Immutable after construction (or use builder pattern)
- Position tracking (source file, line, column, offset, length)
- No inheritance from `Node` - completely independent hierarchy
- Visitor pattern for traversal

Key types:
- **`Program`** - top-level script
- **Statements:** `BlockStmt`, `VarDecl`, `IfStmt`, `ForStmt`, `ForInStmt`, `ForOfStmt`,
  `WhileStmt`, `DoWhileStmt`, `SwitchStmt`, `TryStmt`, `ThrowStmt`, `ReturnStmt`,
  `BreakStmt`, `ContinueStmt`, `WithStmt`, `LabeledStmt`, `ExprStmt`, `EmptyStmt`,
  `FunctionDecl`, `ClassDecl`
- **Expressions:** `BinaryExpr`, `UnaryExpr`, `UpdateExpr`, `AssignExpr`, `CallExpr`, `NewExpr`,
  `MemberExpr`, `ComputedMemberExpr`, `ConditionalExpr`, `SequenceExpr`, `ArrowFuncExpr`,
  `FunctionExpr`, `TemplateLiteral`, `TaggedTemplate`, `YieldExpr`, `AwaitExpr`, `SpreadExpr`,
  `ObjectExpr`, `ArrayExpr`
- **Literals:** `NumberLit`, `StringLit`, `BooleanLit`, `NullLit`, `RegExpLit`, `BigIntLit`
- **Patterns:** `ObjectPattern`, `ArrayPattern`, `AssignPattern`, `RestElement`
- **Other:** `Identifier`, `Property`, `ClassBody`, `MethodDef`, `CatchClause`, `SwitchCase`

Also provide:
- **`AstVisitor`** interface with default methods
- **`SourceLocation`** record for position tracking
- **`AstPrinter`** for debugging

**Files to create:** ~30-40 files in `rhino/src/main/java/org/mozilla/javascript/compiler/ast/`

---

## Phase 6: New Lexer

**Goal:** Clean, modern lexer with its own token types.

**Package:** `org.mozilla.javascript.compiler.parser`

- **`TokenType`** - enum with all token types (not shared with IR or interpreter)
- **`Token`** - record with type, value, position
- **`Lexer`** - produces token stream from source string
- Full ES2024+ support
- Clean handling of: template literals, regexp literals, automatic semicolon insertion (ASI),
  unicode escapes, numeric separators, BigInt literals

**Files to create:**
- `rhino/src/main/java/org/mozilla/javascript/compiler/parser/TokenType.java`
- `rhino/src/main/java/org/mozilla/javascript/compiler/parser/Token.java`
- `rhino/src/main/java/org/mozilla/javascript/compiler/parser/Lexer.java`
- Extensive lexer tests

---

## Phase 7: New Parser

**Goal:** Recursive descent parser producing new AST.

**Package:** `org.mozilla.javascript.compiler.parser`

- Consumes token stream from Lexer
- Produces new AST types from Phase 5
- Error recovery for IDE mode
- Clear error messages with source locations
- Full ES2024+ support

**Files to create:**
- `rhino/src/main/java/org/mozilla/javascript/compiler/parser/Parser.java`
- `rhino/src/main/java/org/mozilla/javascript/compiler/parser/ParseError.java`
- Extensive parser tests

---

## Phase 8: AST → IR Lowering

**Goal:** Transform new AST into the CFG-based IR.

**Package:** `org.mozilla.javascript.compiler.lowering`

This is the equivalent of `IRFactory` but producing CFG IR instead of the Node tree. It handles:

- **Desugaring**: for-of → iterator protocol, destructuring → property accesses, default params →
  conditional assignments, template literals → string concatenation, class → constructor function +
  prototype, optional chaining → null checks + conditional jumps
- **Scope analysis**: determine which variables are local vs closure-captured vs global, detect
  `eval`/`with` usage that forces dynamic scope
- **Control flow construction**: build CFG from structured control flow, wire exception handlers
- **Generator transformation**: convert generator bodies into state machines

**Files to create:**
- `rhino/src/main/java/org/mozilla/javascript/compiler/lowering/AstToCfg.java`
- `rhino/src/main/java/org/mozilla/javascript/compiler/lowering/ScopeAnalyzer.java`
- `rhino/src/main/java/org/mozilla/javascript/compiler/lowering/Desugarer.java` (or inline in AstToCfg)
- Tests

---

## Phase 9: Integration and Switchover

**Goal:** Wire the new pipeline into `Context`, make it the default, remove old code.

### Step 1: Parallel pipelines
- Add a configuration flag (system property or `Context` method) to select old vs new pipeline
- New pipeline: new Lexer → new Parser → new AST → `AstToCfg` → new IR → `CfgToIcode` or `CfgToJvm`
- Run full test suite with new pipeline

### Step 2: Switch default
- Make new pipeline the default
- Old pipeline still available as fallback

### Step 3: Remove old pipeline
- Delete: `TokenStream.java`, `Parser.java`, `IRFactory.java`, `CodeGenerator.java`,
  `NodeTransformer.java`, old `Codegen.java`, `BodyCodegen.java`
- Delete: `org.mozilla.javascript.ast` package (all 79 classes)
- Delete: `Node.java` (or retain minimal version if runtime still needs it)
- Clean up `Token.java` - the interpreter still uses Token constants for Icode, so this may need to
  stay but can be renamed/cleaned
- Delete: Phase 2 adapter (`NodeToCfg`)
- Update `Context.java` to only use new pipeline
- Remove unused `CompilerEnvirons` fields
- Update all imports and references

### Step 4: Clean up
- Run full test suite
- Run spotless
- Update documentation

**Key files to modify:**
- `rhino/src/main/java/org/mozilla/javascript/Context.java` (compilation orchestration)
- `rhino/src/main/java/org/mozilla/javascript/CompilerEnvirons.java`

---

## Package Structure Summary

```
org.mozilla.javascript.compiler
├── ast/           (Phase 5: new AST types)
├── parser/        (Phase 6-7: new lexer + parser)
├── ir/            (Phase 1: CFG-based IR)
├── lowering/      (Phase 8: AST → IR)
├── backend/
│   ├── interp/    (Phase 3: IR → Icode)
│   └── jvm/       (Phase 4: IR → JVM bytecode)
└── adapter/       (Phase 2: temporary old-IR → new-IR bridge)
```

---

## Verification Strategy

Each phase has its own testability:

| Phase | How to test                                                              |
|-------|---------------------------------------------------------------------------------|
| 0 ✅  | `./gradlew check` passes on Java 17                                            |
| 1 ✅  | Unit tests for IR construction, validation, printing                            |
| 2 | Full test suite via: old parser → old IR → adapter → validate IR |
| 3 | Full test suite via: old pipeline → adapter → new Icode backend → Interpreter |
| 4 | Full test suite via: old pipeline → adapter → new JVM backend |
| 5 | Unit tests for AST construction and visitor |
| 6 | Unit tests for lexer against all token types |
| 7 | Parser tests: source → AST → `AstPrinter` output comparison |
| 8 | Full test suite via: new parser → new lowering → new IR → both backends |
| 9 | Full test suite with old pipeline removed |

At every phase, `./gradlew check` must pass.

---

## Risks and Mitigations

1. **Scope creep**: Each phase is independently valuable. If the project stalls, partial progress
   (e.g., new IR + new backends) still improves the codebase.

2. **Old IR adapter fidelity**: The `NodeToCfg` adapter might not handle all Node patterns correctly.
   Mitigation: run the full test suite early and often; fix adapter bugs as they surface.

3. **Icode compatibility**: The new Icode generator must produce bytecode that the existing
   `Interpreter` executes identically. Mitigation: differential testing - run same programs through
   old and new paths, compare results.

4. **Exception handling in CFG**: try/catch/finally is notoriously tricky in CFG form. Mitigation:
   use an exception handler table (like JVM) rather than trying to represent it in the CFG edges.

5. **Generator state machines**: Converting generator functions to resumable state machines in the
   new IR is complex. Mitigation: study existing `BodyCodegen` generator support carefully; consider
   deferring generator support to a sub-phase.
