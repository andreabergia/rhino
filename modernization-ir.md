# Phase 1: Define the New CFG-based IR

## Context

This is Phase 1 of the Rhino compiler pipeline modernization (see `modernization.md`). Phase 0 (Java 17 upgrade) is complete. The goal is to create the new IR data structures and utilities in a new `org.mozilla.javascript.compiler.ir` package, with **no connections to the existing pipeline yet**. This lays the foundation for all subsequent phases.

The IR uses **basic blocks in SSA form** (Static Single Assignment). Each function/script is a `CfgFunction` containing `BasicBlock`s. Each block has a list of `Instruction`s (starting with Phi nodes at join points) and one `Terminator`. Instructions produce values into `Register`s; each register is defined exactly once (SSA property).

SSA is chosen because the key long-term driver is optimization passes (constant propagation, dead code elimination, type inference, etc.) which all benefit from SSA's single-definition property.

**Phase 1 scope**: Define all IR types, the builder (produces non-SSA IR), printer, validator (with SSA validation mode), and a `CfgAnalysis` utility. The actual SSA construction algorithm (dominance computation, phi insertion, renaming) is deferred to Phase 2.

All types use Java 17 sealed interfaces and records for exhaustive pattern matching.

---

## Files to Create

All source in `rhino/src/main/java/org/mozilla/javascript/compiler/ir/`.
All tests in `rhino/src/test/java/org/mozilla/javascript/compiler/ir/`.

### Implementation Order (by dependency)

| Step | File | Description |
|------|------|-------------|
| 1 | `Register.java` | `record Register(int id)` - virtual register |
| 2 | `BlockId.java` | `record BlockId(int id)` - basic block identifier |
| 3 | `BinOp.java` | Enum: ADD, SUB, MUL, DIV, MOD, EXP, BITOR, BITXOR, BITAND, LSH, RSH, URSH, EQ, NE, SHEQ, SHNE, LT, LE, GT, GE, IN, INSTANCEOF |
| 4 | `UnOp.java` | Enum: NEG, POS, NOT, BITNOT, TYPEOF, VOID |
| 5 | `IncDecOp.java` | Enum: PRE_INC, PRE_DEC, POST_INC, POST_DEC |
| 6 | `EnumKind.java` | Enum: KEYS, VALUES, ARRAY, VALUES_IN_ORDER (for-in/for-of iteration) |
| 7 | `CallKind.java` | Enum: NORMAL, EVAL, SUPER |
| 8 | `LiteralPropertyKind.java` | Enum: VALUE, GETTER, SETTER, METHOD, SPREAD |
| 9 | `IrConstant.java` | Sealed interface: NumberConst, StringConst, BigIntConst, BooleanConst, NullConst, UndefinedConst, RegExpConst |
| 10 | `Instruction.java` | Sealed interface with ~53 record variants including Phi and Move (see below) |
| 11 | `Terminator.java` | Sealed interface: Jump, CondJump, CondJumpNullUndef, Return, ReturnVoid, Throw, Rethrow, Switch, GoSub |
| 12 | `BasicBlock.java` | `record BasicBlock(BlockId id, List<Instruction> instructions, Terminator terminator)` |
| 13 | `ExceptionHandler.java` | `record ExceptionHandler(List<BlockId> protectedBlocks, BlockId handlerBlock, boolean isFinally)` |
| 14 | `CfgFunction.java` | Top-level container record with `isSsa` flag |
| 15 | `CfgAnalysis.java` | Predecessor/successor maps + reverse postorder traversal |
| 16 | `CfgBuilder.java` | Fluent builder API (produces non-SSA IR; SSA construction deferred to Phase 2) |
| 17 | `IrPrinter.java` | Human-readable textual dump |
| 18 | `IrValidator.java` | Structural invariant checker with `NON_SSA` and `SSA` validation modes |
| 19 | `CfgBuilderTest.java` | Builder + IR construction tests |
| 20 | `IrPrinterTest.java` | Printer output tests |
| 21 | `IrValidatorTest.java` | Validation tests (both modes) |

Steps 1-9 are independent. Steps 10-11 depend on 1-9. Steps 12-13 depend on 10-11. Step 14 depends on 12-13. Step 15 depends on 2, 11, 12, 14. Step 16 depends on 14. Steps 17-18 depend on 14-15.

---

## Instruction Set

Organized by category. Instructions with a `dest` register produce a value; those without are side-effect only.

### SSA
- `Phi(Register dest, List<PhiInput> inputs)` - SSA phi function at join points; selects value based on predecessor. `PhiInput(BlockId predecessor, Register value)` is a supporting record. Phis must appear at the beginning of a block, before any non-Phi instruction.
- `Move(Register dest, Register src)` - register-to-register copy (used in SSA destruction and general lowering)

### Source Info
- `Line(int lineNumber)` - source line tracking
- `Debugger()` - debugger statement

### Constants
- `LoadConstant(Register dest, IrConstant value)` - load number/string/bigint/bool/null/undefined/regexp
- `LoadTemplateLiteral(Register dest, int templateIndex)` - template literal callsite object

### Local Variable Access (known index, fast path)
- `GetVar(Register dest, int varIndex)`
- `SetVar(int varIndex, Register src)`
- `SetConstVar(int varIndex, Register src)` - const variable write

### Scope Chain Access (dynamic name lookup)
- `GetName(Register dest, String name)` - throws if not found
- `SetName(String name, Register scope, Register value)`
- `StrictSetName(String name, Register scope, Register value)`
- `SetConst(String name, Register scope, Register value)`
- `BindName(Register dest, String name)` - resolve scope object for assignment
- `TypeOfName(Register dest, String name)` - typeof on name (no throw if undeclared)
- `DeleteName(Register dest, String name)`

### Property Access
- `GetProp(Register dest, Register object, String name)`
- `GetPropNoWarn(Register dest, Register object, String name)`
- `GetPropSuper(Register dest, Register object, String name)`
- `SetProp(Register dest, Register object, String name, Register value)` - dest = assigned value
- `SetPropSuper(Register dest, Register object, String name, Register value)`
- `GetElem(Register dest, Register object, Register index)`
- `GetElemSuper(Register dest, Register object, Register index)`
- `SetElem(Register dest, Register object, Register index, Register value)`
- `SetElemSuper(Register dest, Register object, Register index, Register value)`
- `DeleteProp(Register dest, Register object, String name)`
- `DeleteElem(Register dest, Register object, Register index)`
- `DeletePropSuper(Register dest)`
- `GetPropOptional(Register dest, Register object, String name)` - `?.`
- `GetElemOptional(Register dest, Register object, Register index)` - `?.[]`

### Operators
- `BinaryOp(Register dest, BinOp op, Register left, Register right)`
- `UnaryOp(Register dest, UnOp op, Register operand)`

### Increment/Decrement
- `IncDecVar(Register dest, IncDecOp op, int varIndex)`
- `IncDecName(Register dest, IncDecOp op, String name)`
- `IncDecProp(Register dest, IncDecOp op, Register object, String name)`
- `IncDecElem(Register dest, IncDecOp op, Register object, Register index)`

### Function/Method Resolution (for calls)
These resolve a callable + its `this` binding. Needed because JS call semantics depend on how the function was found.
- `NameAndThis(Register destFn, Register destThis, String name, boolean optional)`
- `PropAndThis(Register destFn, Register destThis, Register object, String name, boolean optional)`
- `ElemAndThis(Register destFn, Register destThis, Register object, Register index, boolean optional)`
- `ValueAndThis(Register destFn, Register destThis, Register value, boolean optional)`

### Calls
- `Call(Register dest, Register function, Register thisObj, List<Register> args, CallKind callKind)`
- `New(Register dest, Register constructor, List<Register> args)`

### Object/Array Literals
- `NewArray(Register dest, int size)`
- `NewObject(Register dest)`
- `InitProp(Register object, IrConstant key, Register value, LiteralPropertyKind kind)`
- `InitComputedProp(Register object, Register key, Register value, LiteralPropertyKind kind)`
- `InitArrayElement(Register array, Register value)`
- `ArraySpread(Register array, Register iterable)`
- `ObjectSpread(Register object, Register source)`
- `ObjectRest(Register dest, Register source, List<String> excludedKeys, List<Register> computedExcludedKeys)`

### Closures
- `CreateClosure(Register dest, int functionIndex, boolean isStatement, boolean isMethod)`

### Scope
- `EnterWith(Register object)`
- `LeaveWith()`
- `EnterCatch(Register dest, String name, int scopeIndex, Register exceptionObject)`

### Enumeration (for-in / for-of)
- `EnumInit(Register dest, Register object, EnumKind kind)`
- `EnumNext(Register dest, Register enumState)`
- `EnumId(Register dest, Register enumState)`

### Special Values
- `GetThis(Register dest)`
- `GetThisFn(Register dest)` - function's own name reference
- `GetSuper(Register dest)`
- `GetNewTarget(Register dest)`

### Generators
- `Yield(Register dest, Register value)`
- `YieldStar(Register dest, Register iterable)`
- `GeneratorStart()`
- `GeneratorReturn(Register value)`
- `GeneratorEnd()`

### Ref Operations (XML/special properties)
- `GetRef(Register dest, Register ref)`
- `SetRef(Register dest, Register ref, Register value)`
- `DeleteRef(Register dest, Register ref)`
- `RefSpecial(Register dest, Register object, String name)`

---

## Terminator Set

- `Jump(BlockId target)` - unconditional
- `CondJump(Register condition, BlockId ifTrue, BlockId ifFalse)` - truthy/falsy branch
- `CondJumpNullUndef(Register value, BlockId ifNullUndef, BlockId ifNotNullUndef)` - for `?.` and `??`
- `Return(Register value)`
- `ReturnVoid()`
- `Throw(Register value, int lineNumber)`
- `Rethrow(Register exceptionObject)` - re-throw caught exception
- `Switch(Register value, List<SwitchCase> cases, BlockId defaultTarget)` - with `SwitchCase(Register caseValue, BlockId target)`
- `GoSub(BlockId target, BlockId continuation)` - for finally blocks

---

## CfgFunction Record

```java
record CfgFunction(
    String name,                         // nullable for anonymous
    String sourceName,                   // file name
    int baseLineNumber, int endLineNumber,
    int paramCount,
    String[] variableNames,              // params + locals
    boolean[] isConst,                   // parallel array
    boolean isStrict, boolean isGenerator, boolean isES6Generator,
    boolean isArrow, boolean isMethod, boolean isExpressionClosure,
    boolean needsActivation, boolean hasRestParameter,
    FunctionKind functionKind,            // STATEMENT, EXPRESSION, EXPRESSION_STATEMENT, ARROW
    BlockId entryBlock,
    List<BasicBlock> blocks,
    List<ExceptionHandler> exceptionHandlers,
    List<CfgFunction> nestedFunctions,
    int registerCount,
    boolean isSsa                        // true if in SSA form
)
```

---

## CfgAnalysis

Computes and caches derived CFG information from a `CfgFunction`.

```java
public final class CfgAnalysis {
    public static CfgAnalysis compute(CfgFunction function);

    public List<BlockId> getPredecessors(BlockId block);
    public List<BlockId> getSuccessors(BlockId block);
    public List<BlockId> getReversePostOrder();
}
```

Builds predecessor/successor maps by walking all terminators. Computes reverse postorder via DFS from entry block. Used by the validator (e.g., to check phi predecessors) and will be used by SSA construction in Phase 2.

---

## CfgBuilder API

```java
public final class CfgBuilder {
    CfgBuilder(String name, String sourceName);

    Register newRegister();
    BlockId newBlock();
    void setCurrentBlock(BlockId block);
    BlockId currentBlockId();
    void emit(Instruction instruction);
    void terminate(Terminator terminator);
    void addExceptionHandler(ExceptionHandler handler);
    int addNestedFunction(CfgFunction fn);

    // Metadata setters
    void setParamCount(int), setVariableNames(String[]), setIsConst(boolean[]),
         setStrict(boolean), setGenerator(boolean), setES6Generator(boolean),
         setArrow(boolean), setMethod(boolean), setExpressionClosure(boolean),
         setNeedsActivation(boolean), setHasRestParameter(boolean),
         setFunctionKind(FunctionKind), setBaseLineNumber(int), setEndLineNumber(int);

    CfgFunction build();  // validates all blocks have terminators, freezes lists, isSsa=false
}
```

The builder always produces non-SSA IR (`isSsa=false`). SSA construction is a separate pass (deferred to Phase 2).

---

## IrPrinter Output Format

Example for `function add(a, b) { return a + b; }`:
```
function add(a, b)  // test.js:1-3
  strict=false generator=false arrow=false ssa=false
  params=2 vars=[a, b] registers=3
  entry=B0

  B0:
    line 2
    r0 = getvar 0           // a
    r1 = getvar 1           // b
    r2 = binop ADD r0, r1
    return r2
```

Example with phi nodes (SSA form):
```
  B3:
    r5 = phi(B1: r2, B2: r4)
    r6 = phi(B1: r3, B2: r7)
    r8 = binop ADD r5, r6
    return r8
```

Uses `instanceof` pattern matching (if-else chains) on sealed interfaces for dispatch. Note: `switch` pattern matching would allow compile-time exhaustiveness checks but requires Java 21+; Rhino targets Java 17.

---

## IrValidator

```java
public final class IrValidator {
    public enum Mode { NON_SSA, SSA }
    public static List<String> validate(CfgFunction function, Mode mode);
}
```

### Common checks (both modes)
1. Entry block exists in the block list
2. Every block has a non-null terminator
3. All `BlockId` references in terminators point to existing blocks
4. All register ids are in range `[0, registerCount)`
5. Every register used is defined by some instruction in the function
6. `varIndex` in GetVar/SetVar/etc. is in range `[0, variableNames.length)`
7. `functionIndex` in CreateClosure is in range `[0, nestedFunctions.size())`
8. Exception handler block references are valid
9. `paramCount <= variableNames.length`
10. `isConst.length == variableNames.length`

### SSA-specific checks (Mode.SSA only)
11. Each register is defined by exactly one instruction across the entire function
12. All Phi instructions appear at the beginning of a block, before any non-Phi instruction
13. Each Phi's input count matches the number of predecessor blocks (uses `CfgAnalysis`)
14. Each Phi's input `BlockId`s are exactly the predecessor blocks
15. Entry block has no Phi instructions (no predecessors)

---

## Key Design Decisions

1. **SSA as canonical form** -- the IR is designed for SSA; `isSsa` flag tracks whether a CfgFunction has been converted
2. **Phi as an Instruction variant** -- not a separate list in BasicBlock. Validator enforces placement constraint.
3. **Builder produces non-SSA** -- SSA construction is a separate pass (Phase 2). This keeps the builder simple and the adapter straightforward.
4. **Compound assignments** (+=, etc.) decompose into get + binop + set -- not first-class instructions
5. **Logical operators** (&&, ||, ??) are control flow (CondJump/CondJumpNullUndef), not instructions
6. **Exception handlers** use a side table (not CFG edges) -- matches JVM and existing interpreter model
7. **Generator state machine** transformation is deferred to backends -- IR just records yield points
8. **Records are immutable** -- CfgBuilder is the only mutable API, `build()` freezes everything via `List.copyOf()`
9. **Boolean flags vs EnumSet** -- CfgFunction uses individual boolean fields (isStrict, isGenerator, etc.) for clarity; a future enhancement could consolidate these into an `EnumSet<FunctionFlag>` if the number of flags grows

### Deferred to Phase 2+
- `DominanceInfo` - dominance tree computation (Cooper-Harvey-Kennedy iterative algorithm)
- `SsaConstructor` - non-SSA to SSA conversion (phi insertion + variable renaming)
- `SsaDestructor` - SSA to non-SSA conversion for backends (phi destruction + critical edge splitting)

---

## Tests

### CfgBuilderTest.java
- Build simple function (single block, GetVar + Return)
- Build binary op function (GetVar + BinaryOp + Return)
- Build conditional (multiple blocks with CondJump)
- Build loop pattern (back edges)
- Build with nested function (CreateClosure)
- Build with exception handler
- Build switch
- Verify `build()` fails without terminator
- Verify immutability of returned CfgFunction lists
- Construct one of each Instruction variant
- Construct one of each Terminator variant

### IrPrinterTest.java
- Print simple function, compare against expected output
- Print conditional function, verify block labels
- Print function with exception handlers
- Print phi nodes format
- Verify all instruction formats are readable

### IrValidatorTest.java
- Valid non-SSA function passes (Mode.NON_SSA)
- Valid hand-crafted SSA function passes (Mode.SSA)
- Missing entry block detected
- Dangling block reference detected
- Register out of range detected
- VarIndex out of range detected
- Nested function index out of range detected
- Undefined register use detected
- Invalid exception handler detected
- paramCount > variableNames.length detected
- isConst length mismatch detected
- **SSA**: Duplicate register definition detected
- **SSA**: Phi after non-Phi instruction detected
- **SSA**: Phi input count mismatch detected
- **SSA**: Phi with wrong predecessor BlockIds detected
- **SSA**: Phi in entry block (no predecessors) detected

---

## Verification

```bash
./gradlew check
```

All existing tests continue to pass (new code is isolated). New IR tests validate construction, printing, and validation.
