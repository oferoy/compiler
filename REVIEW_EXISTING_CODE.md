# Review of Given Code (Lexer, Parser, Semantics, IR)

This document summarizes issues in the **existing** skeleton/given code (lexer, parser, semantic analysis, AST→IR). These are not introduced by the register-allocation changes.

---

## 1. Parser (CUP) vs AST op codes – **BUG**

**File:** `cup/CUP_FILE.cup` and `ast/AstExpBinop.java`

- **CUP** assigns: `PLUS→0`, `TIMES→1`, `LT→2`, `EQ→3`.
- **AstExpBinop.irMe()** expects: `0=add`, `1=minus`, `2=mul`, `3=eq`, `4=lt`.

So:

- `*` is given op **1** but AST uses 1 for **minus** and **2** for mul → multiplication is never emitted (no `IrCommandBinopMulIntegers`).
- `<` is given op **2** but AST uses 2 for **mul** and **4** for lt → less-than is compiled as **multiplication**.

**Fix:** In CUP, use the same encoding as the AST, e.g.:

- `PLUS→0`, `MINUS→1`, `TIMES→2`, `EQ→3`, `LT→4`
- Add grammar rules for `exp MINUS exp` and `exp DIVIDE exp` if the language requires them.

---

## 2. Grammar gaps (full L language)

- **No MINUS / DIVIDE** in `binopExp` (only PLUS, TIMES, LT, EQ). The spec mentions integer ops including `-` and `/`.
- **No zero-argument calls:** `callExp` is `ID LPAREN expListComma RPAREN`, so at least one argument is required. So `foo()` is not parseable unless you add a rule like `ID LPAREN RPAREN`.
- **No `nil`, `array`, `new`** in the lexer/grammar if the full L language requires them.

---

## 3. Symbol table

**File:** `symboltable/SymbolTable.java`

- **`void` is not entered.** Comment says “How should we handle void?”. So `void main()` makes `find("void")` return null and semant will fail when checking the return type. **Fix:** e.g. `instance.enter("void", TypeVoid.getInstance());`
- **`PrintString` is not entered.** Spec says both `PrintInt` and `PrintString` are library functions. Only `PrintInt` is in the table. **Fix:** Enter `PrintString` with the appropriate function type.
- **`hash()`** is a stub (e.g. by first letter). Correctness of lookup relies on walking the chain and comparing names; it works but can be inefficient.

---

## 4. Class field lookup – **BUG**

**File:** `ast/AstExpVarField.java`

- `TypeClass.dataMembers` is a `TypeList` of **Type** (e.g. `TypeInt`, `TypeString`). Those types only have a type name (e.g. `"int"`), not the **field name** (e.g. `"i"`).
- The code does `it.head.name == fieldName`. Here `it.head` is the type; `it.head.name` is something like `"int"`, not the field identifier. So the loop never matches a field like `i`, and the comparison is wrong.
- **Fix:** Either store (field name, type) in the class (e.g. a list of `TypeClassVarDec` or similar with `.name` and `.t`) and look up by field name, or resolve field names via a different structure that keeps identifiers.

---

## 5. String comparison

**File:** `ast/AstExpVarField.java`

- `it.head.name == fieldName` uses `==` instead of `.equals()`. Even after fixing the field-name vs type-name issue, this should be `it.head.name.equals(fieldName)` (or the correct field identifier). Same for any other string comparisons in the codebase.

---

## 6. AstExpVarSubscript – missing semant and IR

**File:** `ast/AstExpVarSubscript.java`

- Has **no `semantMe()`** and **no `irMe()`**. So array subscript is neither type-checked nor translated to IR. Any use of `a[i]` will likely fall back to default/undefined behavior or crash (e.g. in assignment/call). Needs semantic checks (index type, array type) and IR for load/store with bounds check if required by the spec.

---

## 7. AstExpVarField – no IR

**File:** `ast/AstExpVarField.java`

- Has **no `irMe()`**. Field access (e.g. `obj.x`) is not translated to IR. Only `AstExpVarSimple` and subscript are given IR in the current design; field access needs load/store from base + offset (or similar) and possibly null-check.

---

## 8. AstStmtAssign – assumes simple variable

**File:** `ast/AstStmtAssign.java`

- `irMe()` does `((AstExpVarSimple) var).name` – **casts `var` to `AstExpVarSimple`**. So assignments like `obj.x := 3` or `a[i] := 5` will throw ClassCastException. Assignments must handle `AstExpVarField` and `AstExpVarSubscript` (and any other var forms) and generate the appropriate store IR.

---

## 9. AstExpCall – only PrintInt, one argument

**File:** `ast/AstExpCall.java`

- **No `semantMe()`** – no check that the function exists and argument types match.
- **irMe()** only: takes `params.head.irMe()` and emits `IrCommandPrintInt(t)`. So every call is treated as `PrintInt(one_arg)`. No support for:
  - Multiple arguments
  - Left-to-right evaluation order
  - Other functions (e.g. `PrintString`, user functions)
  - Zero arguments (and grammar doesn’t allow zero-arg calls anyway)

---

## 10. AstDecFunc.irMe() – only main

**File:** `ast/AstDecFunc.java`

- `irMe()` only adds a label and body for the **current** function and always uses the label `"main"`. So every function gets the label `main`, and there is no distinction between `main` and other functions. You need to emit a label per function (e.g. by name) and only treat `void main()` as the program entry.

---

## 11. Global init order and “main” only

**File:** `ast/AstDecList.java`, `ast/AstDecFunc.java`

- Spec: global variables must be initialized in **order of appearance** before entering `main`. Currently `AstDecList.irMe()` just does `head.irMe(); tail.irMe();` – order is preserved, but there is no explicit “run global inits then jump to main” sequence; that depends on how the single IR list is laid out. If only `main`’s body is ever emitted and globals are mixed in by declaration order, this may be OK, but it’s worth verifying.
- Only one function body is ever emitted (the one that adds `main` and its body). So if the program has multiple functions, only one is compiled; others are never translated to IR.

---

## 12. Parser error reporting and output

**File:** `cup/CUP_FILE.cup`

- `report_error` prints to stdout and calls `System.exit(0)`. It does **not** write `ERROR` or `ERROR(location)` to the **output file**. So the spec’s required output format for syntax errors is not met. The lexer already provides `getLine()` and `getTokenStartPosition()`; those can be used to write e.g. `ERROR(line)\n` to the output file and then exit.

---

## 13. Lexer – strings and keywords

**File:** `jflex/LEX_FILE.lex`

- `STRING = \"[a-z|A-Z]*\"` – only letters (and `|`) inside quotes; no digits or spaces. May be intentional for the course, but the spec says strings can be printed and concatenated; confirm whether string literals are meant to be more general.
- If the full L language has **nil** or **array** or **new**, the lexer (and grammar) need the corresponding tokens and rules.

---

## Summary table

| Component    | Issue                                                                 | Severity |
|-------------|-----------------------------------------------------------------------|----------|
| Parser/AST  | Binop op codes: TIMES/LT wrong; MINUS/DIVIDE missing in grammar      | High     |
| Parser      | No zero-arg call; report_error doesn’t write to output file          | Medium   |
| Symbol table| void and PrintString not entered                                     | High     |
| AstExpVarField | Field lookup uses type name instead of field name; no irMe()      | High     |
| AstExpVarField | String comparison with ==                                           | Medium   |
| AstExpVarSubscript | No semantMe(), no irMe()                                        | High     |
| AstStmtAssign   | Cast to AstExpVarSimple only; breaks field/subscript assignment   | High     |
| AstExpCall     | No semant; IR only for PrintInt(one arg)                           | High     |
| AstDecFunc.irMe | Only one “main” label; no per-function labels or multiple funcs   | High     |

Fixing these will require changes in the **given** lexer, parser, symbol table, and AST (semant + IR), not only in the register allocator or MIPS generator.
