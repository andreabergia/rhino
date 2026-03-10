# Rhino Compiler Pipeline Rewrite

## Context

Rhino's current compiler pipeline suffers from deep structural problems:
- `AstNode extends Node` couples the public AST to the internal IR
- `Token.java` constants are shared across lexer, parser, IR, and interpreter opcodes
- The IR is a weakly-typed tree with gotos encoded as properties — hard to analyze or optimize
- Adding new ES features requires touching many tightly-coupled components
- The pipeline is ~15k+ lines of intertwined code with no clean layer boundaries

This plan replaces the entire compilation pipeline (lexer, parser, AST, IR, code generators) with a
modern, cleanly layered architecture using Java 17+ features (sealed interfaces, records, pattern
matching). The runtime (`ScriptRuntime`, `Interpreter`, `Scriptable`, builtins, etc.) is kept as-is.

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

Set source/target compatibility to Java 17 in `build.gradle` and CI; updated `AGENTS.md`. Enables
sealed interfaces, records, and pattern matching for `instanceof`.

---

## Phase 1: Define the New IR ✅

**Package:** `org.mozilla.javascript.compiler.ir`

The IR uses **basic blocks in SSA form** (Static Single Assignment). Each function/script is a
`CfgFunction` containing `BasicBlock`s. Each block has a list of `Instruction`s (starting with Phi
nodes at join points) and one `Terminator`. Instructions produce values into `Register`s; each
register is defined exactly once (SSA property). An `isSsa` flag tracks whether a function has
been converted. The builder produces non-SSA IR; SSA construction (dominance computation, phi
insertion, renaming) is deferred to a later phase.

All types use Java 17 sealed interfaces and records. Dispatch uses `instanceof` pattern matching
(if-else chains); `switch` pattern matching requires Java 21+.

### Core Types

- **`CfgFunction`** — top-level container:
  ```java
  record CfgFunction(
      String name, String sourceName,
      int baseLineNumber, int endLineNumber,
      int paramCount,
      String[] variableNames, boolean[] isConst,
      boolean isStrict, boolean isGenerator, boolean isES6Generator,
      boolean isArrow, boolean isMethod, boolean isExpressionClosure,
      boolean needsActivation, boolean hasRestParameter,
      FunctionKind functionKind,
      BlockId entryBlock,
      List<BasicBlock> blocks,
      List<ExceptionHandler> exceptionHandlers,
      List<CfgFunction> nestedFunctions,
      int registerCount,
      boolean isSsa
  )
  ```
- **`BasicBlock`** — `record(BlockId id, List<Instruction> instructions, Terminator terminator)`
- **`Register`** — `record(int id)`; **`BlockId`** — `record(int id)`
- **`ExceptionHandler`** — `record(List<BlockId> protectedBlocks, BlockId handlerBlock, boolean isFinally)`

### Instruction Set (55 variants, sealed interface)

Instructions with a `dest` register produce a value; those without are side-effect only.

- **SSA:** `Phi(dest, List<PhiInput>)` (must appear at start of block before any non-Phi),
  `Move(dest, src)`
- **Source info:** `Line(int)`, `Debugger()`
- **Constants:** `LoadConstant(dest, IrConstant)` where `IrConstant` is a sealed interface
  (`NumberConst`, `StringConst`, `BigIntConst`, `BooleanConst`, `NullConst`, `UndefinedConst`,
  `RegExpConst`); `LoadTemplateLiteral(dest, int templateIndex)`
- **Local vars (known index):** `GetVar`, `SetVar`, `SetConstVar`
- **Scope chain:** `GetName`, `SetName`, `StrictSetName`, `SetConst`, `BindName`, `TypeOfName`,
  `DeleteName`
- **Properties:** `GetProp`, `GetPropNoWarn`, `GetPropSuper`, `SetProp`, `SetPropSuper`, `GetElem`,
  `GetElemSuper`, `SetElem`, `SetElemSuper`, `DeleteProp`, `DeleteElem`, `DeletePropSuper`,
  `GetPropOptional`, `GetElemOptional`
- **Operators:** `BinaryOp(dest, BinOp, left, right)` with `BinOp` enum (ADD, SUB, MUL, DIV, MOD,
  EXP, BITOR, BITXOR, BITAND, LSH, RSH, URSH, EQ, NE, SHEQ, SHNE, LT, LE, GT, GE, IN, INSTANCEOF);
  `UnaryOp(dest, UnOp, operand)` with `UnOp` enum (NEG, POS, NOT, BITNOT, TYPEOF, VOID)
- **Inc/dec:** `IncDecVar`, `IncDecName`, `IncDecProp`, `IncDecElem` with `IncDecOp` enum (PRE_INC,
  PRE_DEC, POST_INC, POST_DEC)
- **Function resolution:** `NameAndThis`, `PropAndThis`, `ElemAndThis`, `ValueAndThis` (each
  produces a function register + a `this` register)
- **Calls:** `Call(dest, function, thisObj, List<Register> args, CallKind)` with `CallKind` enum
  (NORMAL, EVAL, SUPER); `New(dest, constructor, List<Register> args)`
- **Literals:** `NewArray`, `NewObject`, `InitProp`, `InitComputedProp` (with
  `LiteralPropertyKind`: VALUE, GETTER, SETTER, METHOD, SPREAD), `InitArrayElement`, `ArraySpread`,
  `ObjectSpread`, `ObjectRest`
- **Closures:** `CreateClosure(dest, functionIndex, isStatement, isMethod)`
- **Scope:** `EnterWith`, `LeaveWith`, `GetCaughtException`, `EnterCatch`
- **Enumeration (for-in / for-of):** `EnumInit(dest, object, EnumKind)` with `EnumKind` enum
  (KEYS, VALUES, ARRAY, VALUES_IN_ORDER), `EnumNext`, `EnumId`
- **Special values:** `GetThis`, `GetThisFn`, `GetSuper`, `GetNewTarget`
- **Generators:** `Yield`, `YieldStar`, `GeneratorStart`, `GeneratorReturn`, `GeneratorEnd`
- **Ref ops:** `GetRef`, `SetRef`, `DeleteRef`, `RefSpecial`

### Terminator Set (sealed interface)

`Jump(target)`, `CondJump(condition, ifTrue, ifFalse)`,
`CondJumpNullUndef(value, ifNullUndef, ifNotNullUndef)`, `Return(value)`, `ReturnVoid()`,
`Throw(value, lineNumber)`, `Rethrow(exceptionObject)`, `Switch(value, List<SwitchCase>, defaultTarget)`
with `SwitchCase(caseValue, target)`, `GoSub(target, continuation)` (for finally blocks).

### Utilities

- **`CfgAnalysis`** — computes predecessor/successor maps and reverse postorder from a
  `CfgFunction`. Used by the validator and (eventually) SSA construction.
- **`CfgBuilder`** — fluent API for constructing `CfgFunction`s (produces non-SSA IR;
  `build()` validates terminators exist, freezes lists via `List.copyOf()`).
- **`IrPrinter`** — human-readable textual dump of the CFG.
- **`IrValidator`** — structural invariant checker with `NON_SSA` and `SSA` modes; recursively
  validates nested functions.

### Validator checks

Common (both modes): entry block exists; every block has a non-null terminator; all `BlockId`
references valid; register ids in `[0, registerCount)`; every used register is defined;
`varIndex` in range; `functionIndex` in `CreateClosure` in range; exception handler blocks valid;
`paramCount <= variableNames.length`; `isConst.length == variableNames.length`.

SSA-only: each register defined exactly once; Phis at start of block; Phi input count matches
predecessor count; Phi input `BlockId`s match predecessors; entry block has no Phis.

### Key Design Decisions

1. **SSA as canonical form** — `isSsa` flag tracks conversion state.
2. **Phi is an Instruction variant** — not a separate list; validator enforces placement.
3. **Builder produces non-SSA** — SSA construction is a separate pass.
4. **Compound assignments** (`+=`, etc.) decompose into get + binop + set — not first-class.
5. **Logical operators** (`&&`, `||`, `??`) are control flow (CondJump/CondJumpNullUndef), not
   instructions.
6. **Exception handlers** use a side table (not CFG edges) — matches JVM and existing interpreter.
7. **Generator state machine** transformation is deferred to backends — IR records yield points.
8. **Records are immutable** — `CfgBuilder` is the only mutable API.

### Deferred (later phase)
- `DominanceInfo` — Cooper-Harvey-Kennedy iterative algorithm
- `SsaConstructor` — non-SSA → SSA (phi insertion + renaming)
- `SsaDestructor` — SSA → non-SSA for backends (phi destruction + critical edge splitting)

**Files:** 19 source files in `rhino/src/main/java/org/mozilla/javascript/compiler/ir/` plus
`CfgBuilderTest`, `IrPrinterTest`, `IrValidatorTest`.

---

## Phase 2: Old IR → New IR Adapter ✅

**Package:** `org.mozilla.javascript.compiler.adapter`

`NodeToCfg.convert(ScriptNode tree, CompilerEnvirons env) → CfgFunction` bridges the existing
pipeline (Parser → IRFactory → NodeTransformer) to the new IR. Temporary/throwaway code, but it
gives us full test coverage immediately.

### Algorithm

Two-pass with block splitting (the old IR uses `TARGET` nodes as jump labels and `GOTO`/`IFEQ`/`IFNE`
with `Jump.target` pointers; forward jumps reference TARGETs not yet visited):

1. **`scanForTargets`** — walk statements recursively; for every `TOKEN.TARGET` allocate a
   `BlockId` via `builder.newBlock()`, store in `Map<Node, BlockId>`.
2. **`visitStatement` / `visitExpression`** — walk again, emitting instructions. Block boundaries
   are created at TARGET nodes, GOTO/IFEQ/IFNE, and inside short-circuit expressions
   (AND/OR/HOOK/NULLISH_COALESCING).

### Notable handling

- **Register allocation:** monotonic counter, no reuse (throwaway code).
- **Script result:** dedicated result register; `EXPR_RESULT` stores into it; script ends with
  `Return(resultReg)`.
- **`SETPROP_OP`/`SETELEM_OP`/`SET_REF_OP`:** get current value → set `useStackRegister` → eval
  RHS → set; `USE_STACK` reads `useStackRegister`.
- **Properties:** super access and optional chaining handled (block-splitting to skip evaluation).
- **Calls:** optional chaining splits blocks to skip argument evaluation; `generateCallFunAndThis`
  resolves function + `this`.
- **Try/catch/finally:** TRY pushes a `TryContext`; `switchToBlock()` adds each new block to
  `protectedBlocks`; on pop, emits `ExceptionHandler(protectedBlocks, catchBlock, isFinally)`.
  Catch landing pad emits `GetCaughtException` before jumping to the real handler TARGET. Finally
  entered via `GoSub` (normal) or exception handler (exceptional).
- **Generators:** parameter init block, closure statements, `GeneratorStart` prologue.
- **Nested `FunctionNode`s:** processed recursively via `emitNestedFunctions`.
- **`STRING_CONCAT`:** mapped to `BinOp.ADD` (runtime handles type coercion).
- **XML nodes** (DOTQUERY, REF_MEMBER, etc.): throw `UnsupportedOperationException`.

### Testing

61 JUnit 5 tests parse JS source through the full pipeline (Parser → IRFactory → NodeTransformer →
NodeToCfg) and assert `IrValidator.validate(fn, Mode.NON_SSA)` returns no errors. Coverage:
constants, unary/binary ops, typeof, void, variables, control flow (if/while/for/break),
short-circuit operators, ternary, nullish coalescing, comma, functions
(declaration/expression/arrow/IIFE), calls, properties, delete, increment, compound assignment,
try/catch/finally, throw, for-in, with, generators, switch, nested functions, complex programs
(fibonacci, control flow combos).

**Files:**
- `rhino/src/main/java/org/mozilla/javascript/compiler/adapter/NodeToCfg.java` (~1050 lines)
- `rhino/src/test/java/org/mozilla/javascript/compiler/adapter/NodeToCfgTest.java` (~350 lines)

---

## Phase 3: New IR → Icode Backend (in progress)

**Goal:** Generate Icode bytecode from the new IR, targeting the existing `Interpreter`.

**Package:** currently in `org.mozilla.javascript`; intended package
`org.mozilla.javascript.compiler.backend.interp`.

`CfgToIcode` takes a `CfgFunction` and produces `InterpreterData` (the existing bytecode format
consumed by `Interpreter.java`):

- Linearize the CFG (order basic blocks for sequential execution)
- Convert registers to stack operations (register-to-stack lowering)
- Emit Icode/Token bytecodes for each instruction
- Generate string/number/bigint literal tables
- Generate exception handler tables
- Handle generator save/restore points

`NewInterpreterEvaluator` wires it into `Context`:
1. Receives a `ScriptNode` from `Context.parse()` (old parser still in use)
2. Runs `NodeToCfg` (Phase 2 adapter)
3. Runs `CfgToIcode`
4. Produces `JSDescriptor` with the generated `InterpreterData`

### Current Status (2026-03-10)

All 66 `CfgToIcodeTest` tests pass.

**Completed:**
- Phase 3a: Minimal backend — literals, arithmetic, variables, return
- Phase 3b: Core features — properties, calls, control flow, for-in, inc/dec, switch, closures
- Phase 3c: Advanced features — try/catch/finally, typeof, delete, regexp, arrow functions,
  nested functions, this binding
- Fixed `NodeToCfg` protected blocks for try-catch-finally and nested try-catch
- Fixed `CfgToIcode.emitEnterCatch` (CATCH_SCOPE local slot handling)
- Removed `NodeToCfg` dependency from `CfgToIcode` (decycle fix via converter function)

### TODO

**Immediate**
- [ ] Run `./gradlew spotlessApply` (pre-existing JDK compat issue with google-java-format)
- [ ] Run full Rhino test suite with new backend enabled to measure coverage

**Phase 3c gaps** (audit remaining IR instructions not yet handled in `CfgToIcode`)
- [ ] `with` statement support
- [ ] const variable enforcement (`SetConstVar`)
- [ ] `StrictSetName`
- [ ] super property access
- [ ] Optional chaining (`GetPropOptional`, `GetElemOptional`)
- [ ] BigInt support
- [ ] Template literals (`LoadTemplateLiteral`)
- [ ] Ref operations (`GetRef`, `SetRef`, `DeleteRef`, `RefSpecial`)

**Phase 3d: Generators (deferred)**
- [ ] `GeneratorStart`, `GeneratorEnd`, `GeneratorReturn`
- [ ] `Yield`, `YieldStar`
- [ ] Generator prologue (param init + closure statements + `Icode_GENERATOR`)

**Polish**
- [ ] Full test262 suite comparison (old vs new backend)
- [ ] Performance benchmarks

**Key reference files (old generator being replaced):**
- `rhino/src/main/java/org/mozilla/javascript/CodeGenerator.java` (2,014 lines)
- `rhino/src/main/java/org/mozilla/javascript/InterpreterData.java` — output format
- `rhino/src/main/java/org/mozilla/javascript/Interpreter.java` — consumer

---

## Phase 4: New IR → JVM Backend

**Goal:** Generate JVM bytecode from the new IR.

**Package:** `org.mozilla.javascript.compiler.backend.jvm`

`CfgToJvm` takes a `CfgFunction` and produces JVM class files. Consider using the ASM library
instead of the existing `ClassFileWriter` for better maintainability and features.

Generated classes must:
- Implement `JSCode<T>` interface (`execute` and `resume` methods)
- Call `ScriptRuntime` / `OptRuntime` static methods for all operations
- Handle generator state machines for `resume()`
- Produce `JSDescriptor` with all required metadata

`NewCompiledEvaluator` follows the same pattern as Phase 3's `NewInterpreterEvaluator`.

**Testing:** run full test suite through new JVM backend; compare with old `Codegen`/`BodyCodegen`.

**Key reference files:**
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
- No inheritance from `Node` — completely independent hierarchy
- Visitor pattern for traversal

Key types:
- **`Program`** — top-level script
- **Statements:** `BlockStmt`, `VarDecl`, `IfStmt`, `ForStmt`, `ForInStmt`, `ForOfStmt`,
  `WhileStmt`, `DoWhileStmt`, `SwitchStmt`, `TryStmt`, `ThrowStmt`, `ReturnStmt`, `BreakStmt`,
  `ContinueStmt`, `WithStmt`, `LabeledStmt`, `ExprStmt`, `EmptyStmt`, `FunctionDecl`, `ClassDecl`
- **Expressions:** `BinaryExpr`, `UnaryExpr`, `UpdateExpr`, `AssignExpr`, `CallExpr`, `NewExpr`,
  `MemberExpr`, `ComputedMemberExpr`, `ConditionalExpr`, `SequenceExpr`, `ArrowFuncExpr`,
  `FunctionExpr`, `TemplateLiteral`, `TaggedTemplate`, `YieldExpr`, `AwaitExpr`, `SpreadExpr`,
  `ObjectExpr`, `ArrayExpr`
- **Literals:** `NumberLit`, `StringLit`, `BooleanLit`, `NullLit`, `RegExpLit`, `BigIntLit`
- **Patterns:** `ObjectPattern`, `ArrayPattern`, `AssignPattern`, `RestElement`
- **Other:** `Identifier`, `Property`, `ClassBody`, `MethodDef`, `CatchClause`, `SwitchCase`

Also provide: **`AstVisitor`** (with default methods), **`SourceLocation`** record, **`AstPrinter`**.

**Files to create:** ~30-40 files in `rhino/src/main/java/org/mozilla/javascript/compiler/ast/`

---

## Phase 6: New Lexer

**Goal:** Clean, modern lexer with its own token types.

**Package:** `org.mozilla.javascript.compiler.parser`

- **`TokenType`** — enum with all token types (not shared with IR or interpreter)
- **`Token`** — record with type, value, position
- **`Lexer`** — produces token stream from source string
- Full ES2024+ support
- Clean handling of: template literals, regexp literals, ASI, unicode escapes, numeric separators,
  BigInt literals

**Files to create:** `TokenType.java`, `Token.java`, `Lexer.java`, extensive lexer tests.

---

## Phase 7: New Parser

**Goal:** Recursive descent parser producing the new AST.

**Package:** `org.mozilla.javascript.compiler.parser`

- Consumes token stream from Lexer
- Produces new AST types from Phase 5
- Error recovery for IDE mode
- Clear error messages with source locations
- Full ES2024+ support

**Files to create:** `Parser.java`, `ParseError.java`, extensive parser tests.

---

## Phase 8: AST → IR Lowering

**Goal:** Transform new AST into the CFG-based IR.

**Package:** `org.mozilla.javascript.compiler.lowering`

Equivalent of `IRFactory` but producing CFG IR. Handles:

- **Desugaring:** for-of → iterator protocol, destructuring → property accesses, default params →
  conditional assignments, template literals → string concatenation, class → constructor function
  + prototype, optional chaining → null checks + conditional jumps
- **Scope analysis:** determine local vs closure-captured vs global; detect `eval`/`with` usage
  that forces dynamic scope
- **Control flow construction:** build CFG from structured control flow, wire exception handlers
- **Generator transformation:** convert generator bodies into state machines

**Files to create:** `AstToCfg.java`, `ScopeAnalyzer.java`, `Desugarer.java` (or inline), tests.

---

## Phase 9: Integration and Switchover

### Step 1: Parallel pipelines
- Add a configuration flag (system property or `Context` method) to select old vs new pipeline
- New pipeline: new Lexer → new Parser → new AST → `AstToCfg` → new IR → `CfgToIcode` or `CfgToJvm`
- Run full test suite with new pipeline

### Step 2: Switch default
- Make new pipeline the default; old pipeline still available as fallback

### Step 3: Remove old pipeline
- Delete: `TokenStream.java`, `Parser.java`, `IRFactory.java`, `CodeGenerator.java`,
  `NodeTransformer.java`, old `Codegen.java`, `BodyCodegen.java`
- Delete: `org.mozilla.javascript.ast` package (all 79 classes)
- Delete: `Node.java` (or retain minimal version if runtime still needs it)
- Clean up `Token.java` — the interpreter still uses Token constants for Icode, so this may need
  to stay but can be renamed/cleaned
- Delete: Phase 2 adapter (`NodeToCfg`)
- Update `Context.java` to only use new pipeline
- Remove unused `CompilerEnvirons` fields

### Step 4: Clean up
- Run full test suite, run spotless, update documentation

**Key files to modify:** `Context.java`, `CompilerEnvirons.java`.

---

## Package Structure Summary

```
org.mozilla.javascript.compiler
├── ast/           (Phase 5: new AST types)
├── parser/        (Phase 6-7: new lexer + parser)
├── ir/            (Phase 1: CFG-based IR) ✅
├── lowering/      (Phase 8: AST → IR)
├── backend/
│   ├── interp/    (Phase 3: IR → Icode) — in progress
│   └── jvm/       (Phase 4: IR → JVM bytecode)
└── adapter/       (Phase 2: temporary old-IR → new-IR bridge) ✅
```

---

## Verification Strategy

| Phase | How to test                                                                     |
|-------|---------------------------------------------------------------------------------|
| 0 ✅  | `./gradlew check` passes on Java 17                                             |
| 1 ✅  | Unit tests for IR construction, validation, printing                            |
| 2 ✅  | Full test suite via: old parser → old IR → adapter → validate IR                |
| 3     | Full test suite via: old pipeline → adapter → new Icode backend → Interpreter   |
| 4     | Full test suite via: old pipeline → adapter → new JVM backend                   |
| 5     | Unit tests for AST construction and visitor                                     |
| 6     | Unit tests for lexer against all token types                                    |
| 7     | Parser tests: source → AST → `AstPrinter` output comparison                     |
| 8     | Full test suite via: new parser → new lowering → new IR → both backends         |
| 9     | Full test suite with old pipeline removed                                       |

At every phase, `./gradlew check` must pass.

---

## Risks and Mitigations

1. **Scope creep**: Each phase is independently valuable. If the project stalls, partial progress
   (e.g., new IR + new backends) still improves the codebase.

2. **Old IR adapter fidelity**: The `NodeToCfg` adapter might not handle all Node patterns
   correctly. Mitigation: run the full test suite early and often; fix adapter bugs as they
   surface.

3. **Icode compatibility**: The new Icode generator must produce bytecode that the existing
   `Interpreter` executes identically. Mitigation: differential testing — run same programs
   through old and new paths, compare results.

4. **Exception handling in CFG**: try/catch/finally is notoriously tricky in CFG form. Mitigation:
   use an exception handler table (like JVM) rather than trying to represent it in the CFG edges.

5. **Generator state machines**: Converting generator functions to resumable state machines is
   complex. Mitigation: study existing `BodyCodegen` generator support carefully; consider
   deferring generator support to a sub-phase.
