# IR Metadata Migration - Status and Next Steps

## Current Status (as of branch `ir-function-refactor`)

### Completed Work

1. **Created IR Metadata Class Hierarchy:**
   - `IRScriptOrFnMetadata` (abstract base) with `isInStrictMode()`, `getFunctionType()`, `isMethodDefinition()`, `isGenerator()`
   - `IRScriptMetadata` for scripts
   - `IRFunctionMetadata` for functions (adds `index`)

2. **Integration Points Established:**
   - `IRFactory.initFunction()` creates `IRFunctionMetadata` and attaches to placeholder node via `FUNCTION_PROP_V2`
   - `IRFactory.transformScript()` creates `IRScriptMetadata` on root node
   - `ScriptNode` stores parallel list `functionsMetadata` alongside `functions`

3. **Migrated Usages:**
   - `CodeGenerator`: Receives metadata via `compile()`, uses for `isInStrictMode`, `getFunctionType`, `isMethodDefinition`
   - `Codegen` (optimizer): Similar migration for top-level
   - `CodeGenUtils.setConstructor()`: Uses metadata instead of AST node
   - `NodeTransformer`: Uses metadata for strict mode propagation

### Tests Status
All tests pass.

---

## Next Phases

### Phase 2a: Add Missing Fields to IRFunctionMetadata

**Goal:** Expand metadata to cover all fields accessed by backends.

**Fields to add to `IRFunctionMetadata`:**
```java
private final boolean requiresActivation;
private final boolean requiresArgumentObject;
private final int paramCount;
private final int paramAndVarCount;
private final boolean isES6Generator;
private final boolean isShorthand;
private final boolean hasRestParameter;
```

**Files to modify:**
- `IRFunctionMetadata.java` - add fields and update `from()` factory
- `IRScriptOrFnMetadata.java` - add abstract methods if shared

### Phase 2b: Migrate CodeGenUtils Completely

**Goal:** Remove all remaining direct `FunctionNode` access in `CodeGenUtils`.

**Current direct accesses to migrate:**
| Call | Line | Migrate To |
|------|------|------------|
| `fn.getFunctionType()` | 48 | `metadata.getFunctionType()` |
| `fn.requiresActivation()` | 49 | `metadata.requiresActivation()` |
| `fn.requiresArgumentObject()` | 50 | `metadata.requiresArgumentObject()` |
| `fn.getFunctionName()` | 51 | Keep (name is in metadata or keep AST) |
| `fn.getName()` | 52 | `metadata.getName()` |
| `fn.isES6Generator()` | 57 | `metadata.isES6Generator()` |
| `fn.isShorthand()` | 60 | `metadata.isShorthand()` |
| `fn.getParent()` | 24 | Keep (AST structure check) |

**Files to modify:**
- `CodeGenUtils.java`
- `IRFunctionMetadata.java` (if adding `name` field)

### Phase 2c: Migrate CodeGenerator Completely

**Goal:** Remove remaining `scriptOrFn.getXxx()` calls where metadata can provide the data.

**Current direct accesses:**
| Call | Purpose | Action |
|------|---------|--------|
| `scriptOrFn.getParamAndVarCount()` | Variable count | Add to metadata |
| `scriptOrFn.getFunctionCount()` | Nested function count | Keep (structural) |
| `scriptOrFn.getFunctionNode(i)` | Get nested function | Keep (need AST for now) |
| `scriptOrFn.getRegexpCount/String/Flags()` | Regexp literals | Keep or move to metadata later |
| `scriptOrFn.getTemplateLiteralCount/Strings()` | Template literals | Keep or move to metadata later |
| `scriptOrFn.getIndexForNameNode()` | Symbol resolution | Keep (runtime lookup) |

### Phase 3: Tackle OptFunctionNode (Hardest Part)

**Goal:** Decouple `OptFunctionNode` from direct `FunctionNode` access.

**Current state:** `OptFunctionNode` has public field:
```java
public final FunctionNode fnode;
```

**Usages to migrate:**
| Location | Usage | Action |
|----------|-------|--------|
| `Optimizer.java:31` | `fnode.requiresActivation()` | Use metadata |
| `Optimizer.java:43` | `fnode.requiresActivation()` | Use metadata |
| `Codegen.java:240` | `fnode.getFunctionType()` | Use metadata |
| `OptTransformer.java:86` | `fnode.requiresActivation()` | Use metadata |
| `BodyCodegen.java:115` | `scriptOrFn.isInStrictMode()` | Use metadata |
| `BodyCodegen.java:119` | `fnode.requiresArgumentObject()` | Use metadata |
| `BodyCodegen.java:169` | `fnode.getFunctionType()` | Use metadata |
| `BodyCodegen.java:179` | `fnode.requiresActivation()` | Use metadata |
| `BodyCodegen.java:409` | `fnode.getFunctionType()` | Use metadata |
| `BodyCodegen.java:417` | `scriptOrFn.isInStrictMode()` | Use metadata |
| `BodyCodegen.java:421` | `fnode.requiresArgumentObject()` | Use metadata |
| `BodyCodegen.java:692` | `fnode.getFunctionType()` | Use metadata |
| `BodyCodegen.java:974` | `fnode.getFunctionType()` | Use metadata |
| `BodyCodegen.java:2012` | `fnode.isMethodDefinition()` | Use metadata |

**Approach options:**
1. **Add metadata field to OptFunctionNode:**
   ```java
   public final FunctionNode fnode;  // Keep for now
   public final IRFunctionMetadata metadata;  // Add
   ```
   Then gradually migrate accesses to use `metadata` instead of `fnode`.

2. **Create accessor methods that use metadata:**
   ```java
   public boolean requiresActivation() {
       return metadata.requiresActivation();
   }
   ```

**Files to modify:**
- `OptFunctionNode.java`
- `Optimizer.java`
- `Codegen.java`
- `OptTransformer.java`
- `BodyCodegen.java`

### Phase 4: Cleanup

**Goal:** Remove transitional code and standardize.

**Tasks:**
1. Rename `FUNCTION_PROP_V2` to `IR_METADATA_PROP` (or similar)
2. Remove `FUNCTION_PROP` if no longer needed
3. Consider removing `functionsMetadata` list if metadata is always on placeholder nodes
4. Add assertions that backends don't access `FunctionNode` directly (optional)
5. Update documentation

---

## Attention Points

### 1. Strict Mode Mutation Order
Currently `isInStrictMode()` is set on `FunctionNode` in `IRFactory.initFunction()` **before** metadata creation:
```java
if (outerScopeIsStrict && !fnNode.isInStrictMode()) {
    fnNode.setInStrictMode(true);  // Mutates AST
}
// ... later ...
IRFunctionMetadata irFunctionMetadata = IRFunctionMetadata.from(functionIndex, fnNode);
```
This works because metadata copies the value. Be careful if changing the order.

### 2. Duplicate Storage
Metadata is currently stored in two places:
- On placeholder node: `node.getProp(Node.FUNCTION_PROP_V2)`
- In parent's list: `scriptOrFn.getFunctionMetadata(i)`

Both are needed for different access patterns. Keep them in sync.

### 3. OptFunctionNode.fnode is Public
Direct field access `ofn.fnode` is used throughout optimizer code. Cannot simply remove the field - must migrate all usages first.

### 4. Symbol Table Access
`getIndexForNameNode()` is used for runtime variable resolution. This likely needs to stay on `ScriptNode` rather than moving to metadata, as it involves symbol table lookup.

### 5. Regexp/Template Literals
These are stored in `ScriptNode` and accessed by backends. Could move to metadata for full decoupling, but lower priority since they're not function-specific metadata.

### 6. getParent() for Expression Detection
`CodeGenUtils.fillInForNestedFunction()` checks `fn.getParent()` to detect function expressions:
```java
if (!(fnParent instanceof AstRoot || fnParent instanceof Scope || fnParent instanceof Block)) {
    builder.declaredAsFunctionExpression = true;
```
This AST structure check may need to be computed during IR transformation and stored in metadata, or kept as-is since it only reads AST structure.

---

## Data Flow Diagram (Current)

```
Parser
   │
   ▼
FunctionNode (AST)
   │
   ▼
IRFactory.transformFunction()
   ├── Creates IRFunctionMetadata from FunctionNode
   ├── Attaches to placeholder via FUNCTION_PROP_V2
   └── Adds to parent's functionsMetadata list
   │
   ▼
NodeTransformer.transform()
   └── Uses metadata.isInStrictMode() for propagation
   │
   ▼
CodeGenerator / Codegen
   ├── Receives IRScriptMetadata via compile()
   ├── Gets IRFunctionMetadata via:
   │   ├── node.getProp(FUNCTION_PROP_V2)
   │   └── scriptOrFn.getFunctionMetadata(i)
   └── Still accesses FunctionNode for:
       ├── Parameter/variable info
       ├── Nested function list
       └── Regexp/template literals
```

---

## Success Criteria

- [ ] No `fnode.getFunctionType()` calls in optimizer code
- [ ] No `fnode.requiresActivation()` calls in optimizer code
- [ ] No `fnode.isInStrictMode()` calls in backends
- [ ] No `fnode.isMethodDefinition()` calls in backends
- [ ] `OptFunctionNode` either wraps metadata or has accessor methods
- [ ] All tests pass
- [ ] No performance regression
