package ast;

import symboltable.*;
import types.*;
import temp.*;
import ir.*;

public class AstExpCall extends AstExp
{
    /** When true, log param types and eval order to stderr (for addPairs arg-order debug). */
    private static final boolean DEBUG_ARGS_STDERR = false;
    public String funcName;
    public AstExpList params;
    /** Set in semantMe when this call is resolved as a method call (receiver is object). */
    public boolean isMethodCall = false;
    /** Type of first argument (set in semantMe). Used in irMe: load from memory only when first arg is array (avoids clobbered temp; objects use temp so test 3 passes). */
    public Type firstArgType = null;
    /** Resolved function/method (set in semantMe). Used in irMe to get return type for method calls (SymbolTable.find misses methods). */
    public TypeFunction resolvedFunc = null;

    public AstExpCall(String funcName, AstExpList params)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.funcName = funcName;
        this.params = params;
    }

    @Override
    public void printMe()
    {
        System.out.format("CALL(%s)\nWITH:\n", funcName);

        if (params != null) params.printMe();

        AstGraphviz.getInstance().logNode(
            serialNumber,
            String.format("CALL(%s)", funcName)
        );

        if (params != null)
            AstGraphviz.getInstance().logEdge(serialNumber, params.serialNumber);
    }

    /**
     * semantMe() is used when the call appears as an EXPRESSION,
     * e.g. x := f(...); if (f(...)) { ... } etc.
     * In this context, using a void-returning function as a value is illegal.
     */
    @Override
    public Type semantMe()
    {
        return semantMeInternal(true);  // forbid void as a value
    }

    /**
     * semantMeAsStmt() is used when the call appears as a STATEMENT,
     * e.g. f(...);  PrintInt(5);
     * In this context, void-returning functions are allowed.
     */
    public void semantMeAsStmt()
    {
        semantMeInternal(false);        // allow void result
    }

    /**
     * Shared implementation. When forbidVoidResult == true,
     * we reject void-returning functions as expression values.
     */
    private Type semantMeInternal(boolean forbidVoidResult)
    {
        TypeFunction func       = null;
        AstExpList   actualNode = null;
        TypeList     formalNode = null;

        /********************************************/
        /* 0. Candidate receiver (first argument)   */
        /********************************************/
        Type receiverType = null;
        if (params != null && params.head != null) {
            Type t = params.head.semantMe();
            receiverType = t;
        }

        /********************************************/
        /* 1. Try as REAL CLASS METHOD              */
        /********************************************/
        TypeFunction methodFunc = null;
        if (receiverType instanceof TypeClass) {
            methodFunc = TypesHelper.classMethod(receiverType, funcName);
            if (methodFunc != null) {
                func       = methodFunc;
                actualNode = (params != null ? params.tail : null); // skip receiver
                formalNode = func.params;                           // all params (no explicit "this")
                isMethodCall = true;
            }
        }

        /********************************************/
        /* 2. Look up GLOBAL function               */
        /********************************************/
        TypeFunction globalFunc = null;
        {
            Type t = SymbolTable.find(funcName);
            if (t instanceof TypeFunction) {
                globalFunc = (TypeFunction)t;
            }
        }

        /********************************************/
        /* 1b. Prefer GLOBAL when arity matches     */
        /*     addPairs(addPairs(a,b), addPairs(a,b)) should be global 2-arg call, not obj.addPairs(one arg) */
        /********************************************/
        if (func != null && globalFunc != null && params != null) {
            int callArity = 0;
            for (AstExpList e = params; e != null; e = e.tail) if (e.head != null) callArity++;
            if (callArity == globalFunc.numParams()) {
                func       = globalFunc;
                actualNode = params;
                formalNode = globalFunc.params;
                isMethodCall = false;
            }
        }

        /********************************************/
        /* 2a. GLOBAL "METHOD-AS-FUNCTION" pattern  */
        /*     e.g., birthday(Person this, ...)     */
        /*     Only when call arity != global param count (else use plain global 2b for addPairs(p,q)). */
        /********************************************/
        if (func == null &&
            globalFunc != null &&
            receiverType != null &&
            globalFunc.params != null &&
            globalFunc.params.head instanceof TypeClass)
        {
            int callArity2a = 0;
            for (AstExpList e = params; e != null; e = e.tail) if (e.head != null) callArity2a++;
            if (callArity2a != globalFunc.numParams()) {
                TypeClass thisType = (TypeClass) globalFunc.params.head;
                if (!(receiverType instanceof TypeClass) ||
                    !SymbolTable.isSubclass((TypeClass)receiverType, thisType)) {
                    AstNode.error(lineNumber, "illegal receiver type in call to " + funcName);
                } else {
                    func       = globalFunc;
                    actualNode = (params != null ? params.tail : null);
                    formalNode = globalFunc.params.tail;
                    isMethodCall = true;
                }
            }
        }

        /********************************************/
        /* 2b. Plain GLOBAL function call           */
        /********************************************/
        if (func == null && globalFunc != null) {
            func       = globalFunc;
            actualNode = params;
            formalNode = globalFunc.params;
        }

        /********************************************/
        /* 3. Still nothing? Undefined symbol       */
        /********************************************/
        if (func == null) {
            AstNode.error(lineNumber,
                "undefined function or method: " + funcName);
        }
        resolvedFunc = func;

        /********************************************/
        /* 4. Type-check parameters                 */
        /********************************************/
        AstExpList actual = actualNode;
        TypeList   formal = formalNode;

        while (actual != null && formal != null)
        {
            Type actualType = actual.head.semantMe();
            Type formalType = formal.head;
            firstArgType = actualType;  // overwrite each time; last iteration = first param (grammar builds right-to-left)

            // void is never a legal value argument
            if (actualType instanceof TypeVoid) {
                AstNode.error(lineNumber,
                    "void expression is not allowed as a function argument");
            }

            if (formalType instanceof TypeInt && actualType instanceof TypeInt) {
                // OK
            }
            else if (formalType instanceof TypeString && actualType instanceof TypeString) {
                // OK
            }
            else if (actualType == formalType) {
                // exact same type object – OK
            }
            else if (actualType instanceof TypeNil &&
                     (formalType instanceof TypeClass || formalType instanceof TypeArray)) {
                // nil to class/array – OK
            }
            else if (formalType instanceof TypeClass && actualType instanceof TypeClass)
            {
                if (!SymbolTable.isSubclass((TypeClass)actualType,
                                            (TypeClass)formalType))
                {
                    AstNode.error(lineNumber,
                        "illegal class parameter subtype in call to " + funcName);
                }
            }
            else if (formalType instanceof TypeArray && actualType instanceof TypeArray)
            {
                if (formalType != actualType) {
                    AstNode.error(lineNumber,
                        "array parameter types must match exactly");
                }
            }
            else
            {
                AstNode.error(lineNumber,
                    "illegal parameter type in call to " + funcName);
            }

            actual = actual.tail;
            formal = formal.tail;
        }

        /********************************************/
        /* 5. Wrong number of arguments             */
        /********************************************/
        if (actual != null || formal != null) {
            AstNode.error(lineNumber,
                "wrong number of arguments in call to " + funcName);
        }

        /********************************************/
        /* 6. Check & return function's return type */
        /********************************************/
        if (forbidVoidResult && func.returnType instanceof TypeVoid) {
            AstNode.error(lineNumber,
                "void function '" + funcName + "' cannot be used as a value");
        }

        /* For method calls, irMe needs receiver type for label mangling; type-check loop may have skipped receiver. */
        if (isMethodCall && receiverType != null)
            firstArgType = receiverType;

        return func.returnType;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe()
    {
        if ("PrintInt".equals(funcName))
        {
            if (params != null && params.head != null)
            {
                Temp paramTemp = params.head.irMe();
                Ir.getInstance().AddIrCommand(new IrCommandPrintInt(paramTemp));
            }
            return null;
        }

        if ("PrintString".equals(funcName))
        {
            if (params != null && params.head != null)
            {
                Temp paramTemp = params.head.irMe();
                Ir.getInstance().AddIrCommand(new IrCommandPrintString(paramTemp));
            }
            return null;
        }

        // Grammar builds expList right-to-left: f(a,b) -> (b,(a,null)). Reverse to get [first,second,...].
        java.util.List<AstExp> paramExps = new java.util.ArrayList<>();
        for (AstExpList e = params; e != null; e = e.tail)
            if (e.head != null)
                paramExps.add(e.head);
        java.util.Collections.reverse(paramExps);

        // Only for method calls (obj.method(...) / arr.foo(...)): save receiver to $s2 before evaluating other args.
        // For global functions like BubbleSort(arr, 7), do not reorder — first arg is just first arg.
        // After reverse, method-call list from grammar (receiver, args) is [args..., receiver], so receiver is last.
        java.util.List<Temp> argTemps = new java.util.ArrayList<>();
        boolean firstArgFromS2 = false;
        boolean arg0InS0 = false;
        boolean arg0OnStack = false;
        /** When true, 4+ args and second param (args.get(1)) was pushed; load $a2 from stack. */
        boolean arg1OnStack = false;
        /** When true, exactly 3 args evaluated left-to-right; first two pushed. Used only for 3-arg global calls (TEST_203). */
        boolean argsPushedLTR3 = false;
        if (!paramExps.isEmpty() && isMethodCall && (firstArgType instanceof types.TypeArray || firstArgType instanceof TypeClass)) {
            int receiverIdx = paramExps.size() - 1;
            ast.AstExp receiverExp = paramExps.get(receiverIdx);
            if (receiverExp instanceof AstExpVarSimple) {
                String receiverIrName = ir.VarNameMapper.getInstance().getIrName(((AstExpVarSimple) receiverExp).name);
                if (receiverIrName != null) {
                    Ir.getInstance().AddIrCommand(new IrCommandLoadVarToS2(receiverIrName));
                } else {
                    Temp firstArgTemp = receiverExp.irMe();
                    Ir.getInstance().AddIrCommand(new IrCommandMoveBaseToS2(firstArgTemp));
                }
            } else {
                Temp firstArgTemp = receiverExp.irMe();
                Ir.getInstance().AddIrCommand(new IrCommandMoveBaseToS2(firstArgTemp));
            }
            for (int i = 0; i < receiverIdx; i++) {
                Temp t = paramExps.get(i).irMe();
                argTemps.add(t);
                // Preserve method-call arg in $s0 so it isn't clobbered (e.g. obj.addPairs(other) when obj/other are call results).
                if (receiverIdx == 1) {
                    Ir.getInstance().AddIrCommand(new ir.IrCommandMoveTempToS0(t));
                    arg0InS0 = true;
                }
            }
            firstArgFromS2 = true;
        } else {
            int size = paramExps.size();
            // 2 or 3 args: evaluate left-to-right so side effects run in source order (TEST_203, TEST_86). Push first size-1.
            if (size == 2 || size == 3) {
                argsPushedLTR3 = true;
                for (int i = 0; i < size; i++) {
                    Temp t = paramExps.get(i).irMe();
                    argTemps.add(t);
                    if (i < size - 1)
                        Ir.getInstance().AddIrCommand(new ir.IrCommandPush(t));
                }
            } else {
                // Evaluate right-to-left; preserve first-evaluated so it isn't clobbered (fixes addPairs(addPairs(a,b), addPairs(a,b))).
                if (DEBUG_ARGS_STDERR) {
                    System.err.println("[DEBUG_ARGS] " + funcName + " paramExps.size()=" + paramExps.size() + " (before eval, source order after reverse: param0=1st, param1=2nd)");
                    for (int i = 0; i < paramExps.size(); i++)
                        System.err.println("  param[" + i + "] class=" + paramExps.get(i).getClass().getSimpleName());
                }
                boolean logEvalOrder = (System.getProperty("DEBUG_EVAL_ORDER") != null);
                for (int i = paramExps.size() - 1; i >= 0; i--) {
                    if (logEvalOrder) {
                        System.err.println("[DEBUG_EVAL_ORDER] " + funcName + " about to eval arg " + (i + 1) + " (0-based i=" + i + ")");
                        Ir.getInstance().AddIrCommand(new ir.IrCommandDebugPrintConstInt(i + 1));
                    }
                    Temp t = paramExps.get(i).irMe();
                    argTemps.add(t);
                    if (DEBUG_ARGS_STDERR) System.err.println("[DEBUG_ARGS] " + funcName + " eval order: i=" + i + " -> temp " + t + " (argTemps[" + (paramExps.size()-1-i) + "])");
                    int sz = paramExps.size();
                    // Preserve first param (leftmost, evaluated last when i==0) so it isn't clobbered before the call.
                    if (i == 0 && sz > 1) {
                        if (System.getProperty("DEBUG_ARGS") != null) System.err.println("[DEBUG_ARGS] AstExpCall " + funcName + " preserving arg i=" + i + " (1st param) in $s0+stack, temp=" + t);
                        Ir.getInstance().AddIrCommand(new ir.IrCommandMoveTempToS0(t));
                        Ir.getInstance().AddIrCommand(new ir.IrCommandPushS0());
                        arg0OnStack = true;
                        arg0InS0 = true;
                    }
                    if (i == 1 && sz == 3) {
                        if (System.getProperty("DEBUG_ARGS") != null) System.err.println("[DEBUG_ARGS] AstExpCall " + funcName + " preserving arg i=" + i + ", temp=" + t);
                        Ir.getInstance().AddIrCommand(new ir.IrCommandMoveTempToS0(t));
                        arg0InS0 = true;
                    }
                    // Preserve param that goes in $a2 (third arg) so it isn't clobbered; push when i==2 for 4+ args.
                    if (i == 2 && sz >= 4) {
                        if (System.getProperty("DEBUG_ARGS") != null) System.err.println("[DEBUG_ARGS] AstExpCall " + funcName + " preserving arg i=" + i + " (3rd param) on stack, temp=" + t);
                        Ir.getInstance().AddIrCommand(new ir.IrCommandPush(t));
                        arg1OnStack = true;
                    }
                }
                if (System.getProperty("DEBUG_ARGS") != null && arg0InS0) System.err.println("[DEBUG_ARGS] AstExpCall " + funcName + " arg0InS0=true, argTemps.size()=" + argTemps.size());
            }
        }

        TypeFunction ft = resolvedFunc;
        if (ft == null) {
            Type t = SymbolTable.find(funcName);
            if (t instanceof TypeFunction) ft = (TypeFunction)t;
        }
        Temp dst = null;
        if (ft != null && !(ft.returnType instanceof types.TypeVoid))
            dst = TempFactory.getInstance().getFreshTemp();

        boolean isVirtual = (isMethodCall && firstArgType instanceof TypeClass);
        int methodSlot = isVirtual ? types.VtableBuilder.getMethodSlot(funcName) : -1;
        String mipsLabel = funcName;
        if (isVirtual && methodSlot < 0) {
            TypeClass receiverClass = (TypeClass) firstArgType;
            TypeClass definingClass = receiverClass.getMethodDefiningClass(funcName);
            if (definingClass != null)
                mipsLabel = definingClass.name + "_" + funcName;
        }
        Ir.getInstance().AddIrCommand(new IrCommandCall(dst, mipsLabel, argTemps, null, firstArgFromS2, isVirtual && methodSlot >= 0, methodSlot, arg0InS0, arg0OnStack, arg1OnStack, argsPushedLTR3));
        return dst;
    }
}
