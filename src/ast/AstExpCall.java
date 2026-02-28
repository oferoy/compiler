package ast;

import symboltable.*;
import types.*;
import temp.*;
import ir.*;

public class AstExpCall extends AstExp
{
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
        if (receiverType instanceof TypeClass) {
            TypeFunction methodFunc = TypesHelper.classMethod(receiverType, funcName);

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
        if (func == null) {
            Type t = SymbolTable.find(funcName);
            if (t instanceof TypeFunction) {
                globalFunc = (TypeFunction)t;
            }
        }

        /********************************************/
        /* 2a. GLOBAL "METHOD-AS-FUNCTION" pattern  */
        /*     e.g., birthday(Person this, ...)     */
        /********************************************/
        if (func == null &&
            globalFunc != null &&
            receiverType != null &&
            globalFunc.params != null &&
            globalFunc.params.head instanceof TypeClass)
        {
            TypeClass thisType = (TypeClass) globalFunc.params.head;

            // receiver must be subclass of "this" type
            if (!(receiverType instanceof TypeClass) ||
                !SymbolTable.isSubclass((TypeClass)receiverType, thisType))
            {
                AstNode.error(lineNumber,
                    "illegal receiver type in call to " + funcName);
            }

            func       = globalFunc;
            actualNode = (params != null ? params.tail : null);   // skip receiver actual
            formalNode = globalFunc.params.tail;                  // skip "this" formal
            isMethodCall = true;
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

        // When first arg is array, save to $s2 before evaluating other args (avoids temp clobber). Otherwise eval in order.
        java.util.List<Temp> argTemps = new java.util.ArrayList<>();
        boolean firstArgFromS2 = false;
        if (!paramExps.isEmpty() && firstArgType instanceof types.TypeArray) {
            Temp firstArgTemp = paramExps.get(0).irMe();
            Ir.getInstance().AddIrCommand(new IrCommandMoveBaseToS2(firstArgTemp));
            for (int i = 1; i < paramExps.size(); i++)
                argTemps.add(paramExps.get(i).irMe());
            firstArgFromS2 = true;
        } else {
            for (AstExp p : paramExps)
                argTemps.add(p.irMe());
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
        Ir.getInstance().AddIrCommand(new IrCommandCall(dst, mipsLabel, argTemps, null, firstArgFromS2, isVirtual && methodSlot >= 0, methodSlot));
        return dst;
    }
}
