package ast;

import symboltable.*;
import types.*;
import temp.*;
import ir.*;

public class AstDecFunc extends AstDec
{
    public String returnTypeName;
    public String name;
    public AstTypeNameList params;
    public AstStmtList body;

    // Semantic function type (also used when this is a class method)
    public TypeFunction funcType;

    // Track current function for return statements
    public static Type   currentReturnType = null;
    public static String currentFuncName   = null;

    public AstDecFunc(String returnTypeName,
                      String name,
                      AstTypeNameList params,
                      AstStmtList body)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.returnTypeName = returnTypeName;
        this.name = name;
        this.params = params;
        this.body = body;
    }

    @Override
    public void printMe()
    {
        System.out.format("FUNC(%s):%s\n", name, returnTypeName);
        if (params != null) params.printMe();
        if (body   != null) body.printMe();

        AstGraphviz.getInstance().logNode(
            serialNumber,
            String.format("FUNC(%s)\n:%s", name, returnTypeName));

        if (params != null) AstGraphviz.getInstance().logEdge(serialNumber, params.serialNumber);
        if (body   != null) AstGraphviz.getInstance().logEdge(serialNumber, body.serialNumber);
    }

    @Override
    public Type semantMe()
    {
        // Track locals + parameters for this function
        AstDecFunc.beginFunctionScope(this.params);

        // Class methods have implicit "this" (receiver) so it can be looked up and so fields like "grades" are not treated as globals
        if (AstDecClass.currentClassType != null) {
            SymbolTable.enter("this", AstDecClass.currentClassType);
            AstDecFunc.registerLocal("this");
        }

        /* ----------------------------------------------------
         * 1. Resolve return type
         * ---------------------------------------------------- */
        Type returnType;

        if ("void".equals(returnTypeName)) {
            returnType = TypeVoid.getInstance();
        } else if ("int".equals(returnTypeName)) {
            returnType = TypeInt.getInstance();
        } else if ("string".equals(returnTypeName)) {
            returnType = TypeString.getInstance();
        } else {
            returnType = SymbolTable.find(returnTypeName);
            if (returnType == null) {
                AstNode.error(lineNumber,
                    "non-existing return type: " + returnTypeName);
            }
        }

        /* ----------------------------------------------------
         * 2. Build formal parameter type list
         * ---------------------------------------------------- */
        TypeList formalParams = null;
        TypeList last = null;

        for (AstTypeNameList it = params; it != null; it = it.tail)
        {
            Type t;

            if ("int".equals(it.head.type)) {
                t = TypeInt.getInstance();
            } else if ("string".equals(it.head.type)) {
                t = TypeString.getInstance();
            } else if ("void".equals(it.head.type)) {
                AstNode.error(lineNumber, "parameter type cannot be void");
                t = null; // unreachable
            } else {
                t = SymbolTable.find(it.head.type);
                if (t == null) {
                    AstNode.error(lineNumber,
                        "non-existing parameter type: " + it.head.type);
                }
            }

            if (t instanceof TypeVoid) {
                AstNode.error(lineNumber, "parameter type cannot be void");
            }

            TypeList newNode = new TypeList(t, null);

            if (formalParams == null) {
                formalParams = newNode;
            } else {
                last.tail = newNode;
            }
            last = newNode;
        }

        /* ----------------------------------------------------
         * 3. Create TypeFunction
         * ---------------------------------------------------- */
        this.funcType = new TypeFunction(returnType, name, formalParams);

        /* ----------------------------------------------------
         * 4. Register function / method
         * ---------------------------------------------------- */
        if (AstDecClass.currentClassType != null) {
            /****************************************************
             * This is a CLASS METHOD
             ****************************************************/
            TypeClass cls = AstDecClass.currentClassType;

            // Check override against father (if any)
            if (cls.father != null) {
                TypeFunction parentMethod = cls.father.findMethod(name);
                if (parentMethod != null) {
                    if (!sameSignature(parentMethod, this.funcType)) {
                        AstNode.error(lineNumber,
                            "illegal override of method " + name);
                    }
                }
            }

            // (Optional) check duplicate in THIS class:
            for (TypeClassMember m = cls.dataMembers; m != null; m = m.next) {
                if (m.kind == TypeClassMember.METHOD && m.name.equals(name)) {
                    AstNode.error(lineNumber,
                        "method " + name + " already defined in class " + cls.name);
                }
            }

            // Register as a method of this class
            cls.addMethod(name, this.funcType);
        }
        else {
            /****************************************************
             * This is a GLOBAL FUNCTION
             ****************************************************/
            if (SymbolTable.findInCurrentScope(name) != null) {
                AstNode.error(lineNumber,
                    "function already defined: " + name);
            }

            SymbolTable.enter(name, this.funcType);
        }

        /* ----------------------------------------------------
         * 5. Begin function scope and insert parameters
         * ---------------------------------------------------- */
        SymbolTable.beginScope();

        // Save outer "current function" info
        Type   prevReturnType = AstDecFunc.currentReturnType;
        String prevFuncName   = AstDecFunc.currentFuncName;

        // Set current function for return statements
        AstDecFunc.currentReturnType = returnType;
        AstDecFunc.currentFuncName   = name;

        for (AstTypeNameList it = params; it != null; it = it.tail)
        {
            AstTypeName param = it.head;

            Type paramType;
            if ("int".equals(param.type)) {
                paramType = TypeInt.getInstance();
            } else if ("string".equals(param.type)) {
                paramType = TypeString.getInstance();
            } else if ("void".equals(param.type)) {
                AstNode.error(lineNumber, "parameter type cannot be void");
                paramType = null; // unreachable
            } else {
                paramType = SymbolTable.find(param.type);
            }

            if (SymbolTable.findInCurrentScope(param.name) != null) {
                AstNode.error(lineNumber,
                    "duplicate parameter name: " + param.name);
            }

            SymbolTable.enter(param.name, paramType);
        }

        /* ----------------------------------------------------
         * 6. Semant function body
         * ---------------------------------------------------- */
        if (body != null) {
            body.semantMe();
        }

        /* ----------------------------------------------------
         * 7. End function scope, restore current function
         * ---------------------------------------------------- */
        SymbolTable.endScope();

        AstDecFunc.currentReturnType = prevReturnType;
        AstDecFunc.currentFuncName   = prevFuncName;

        AstDecFunc.endFunctionScope();
        return null;
    }

    /* --------------------------------------------------------
     * Helper: compare method signatures (return + param types)
     * -------------------------------------------------------- */
    private boolean sameSignature(TypeFunction a, TypeFunction b)
    {
        if (a == null || b == null) return false;

        // Return type must be exactly the same
        if (a.returnType != b.returnType) {
            return false;
        }

        // Parameter lists must have same length and same types (by identity)
        TypeList pa = a.params;
        TypeList pb = b.params;

        while (pa != null && pb != null) {
            if (pa.head != pb.head) {
                return false;
            }
            pa = pa.tail;
            pb = pb.tail;
        }

        // Both must end at the same time
        return (pa == null && pb == null);
    }

    /****************************************************/
    /* Helpers for tracking locals/params of the        */
    /* function currently being semanted.               */
    /****************************************************/

    // Holds all parameter + local variable names of the function
    // currently in semantMe(). Null when not inside a function.
    private static java.util.Set<String> currentLocalsAndParams = null;

    // Called at the start of semantMe() for a function
    public static void beginFunctionScope(AstTypeNameList paramsList) {
        currentLocalsAndParams = new java.util.HashSet<String>();

        // Add parameter names
        for (AstTypeNameList it = paramsList; it != null; it = it.tail) {
            if (it.head != null && it.head.name != null) {
                currentLocalsAndParams.add(it.head.name);
            }
        }
    }

    // Called at the end of semantMe() for that function
    public static void endFunctionScope() {
        currentLocalsAndParams = null;
    }

    // Called from AstDecVar.semantMe() for each variable declaration
    public static void registerLocal(String name) {
        if (currentLocalsAndParams != null && name != null) {
            currentLocalsAndParams.add(name);
        }
    }

    // Used by AstExpVarSimple to decide if a name is a local/param
    public static boolean isCurrentLocalOrParam(String name) {
        return currentLocalsAndParams != null &&
            currentLocalsAndParams.contains(name);
    }

    /*****************/
    /* IR ME         */
    /*****************/
    /** Build params in declaration order (grammar builds funcArgs with last param at head). */
    private java.util.List<AstTypeName> paramsInDeclarationOrder()
    {
        java.util.List<AstTypeName> list = new java.util.ArrayList<>();
        for (AstTypeNameList p = params; p != null; p = p.tail)
            if (p.head != null)
                list.add(p.head);
        java.util.Collections.reverse(list);
        return list;
    }

    @Override
    public Temp irMe()
    {
        VarNameMapper.getInstance().beginScope();

        boolean isClassMethod = (AstDecClass.currentClassType != null);
        int paramOffset = isClassMethod ? 1 : 0;

        // Class methods: implicit "this" at $a0 (index 0)
        if (isClassMethod) {
            String thisIrName = VarNameMapper.getInstance().registerVariable("this", this.serialNumber * 1000 - 1);
            Ir.getInstance().AddIrCommand(new IrCommandAllocate(thisIrName));
        }

        java.util.List<AstTypeName> declOrder = paramsInDeclarationOrder();
        for (int idx = 0; idx < declOrder.size(); idx++)
        {
            AstTypeName param = declOrder.get(idx);
            if (param.name != null)
            {
                String irName = VarNameMapper.getInstance().registerVariable(param.name, this.serialNumber * 1000 + idx);
                Ir.getInstance().AddIrCommand(new IrCommandAllocate(irName));
            }
        }

        String endLabel = IrCommand.getFreshLabel("end");
        Ir.setCurrentFunctionEndLabel(endLabel);

        String labelName = name;
        if (isClassMethod && AstDecClass.currentClassType != null)
            labelName = AstDecClass.currentClassType.name + "_" + name;
        Ir.getInstance().AddIrCommand(new IrCommandLabel(labelName));

        // Store incoming $a0 (receiver for methods), $a1,$a2,$a3 into param slots
        if (isClassMethod) {
            String thisIrName = VarNameMapper.getInstance().getIrName("this");
            if (thisIrName != null)
                Ir.getInstance().AddIrCommand(new IrCommandStoreParam(thisIrName, 0));
        }
        for (int idx = 0; idx < declOrder.size(); idx++)
        {
            AstTypeName param = declOrder.get(idx);
            if (param.name != null)
            {
                String irName = VarNameMapper.getInstance().getIrName(param.name);
                if (irName != null)
                    Ir.getInstance().AddIrCommand(new IrCommandStoreParam(irName, idx + paramOffset));
            }
        }

        if (body != null)
            body.irMe();

        Ir.getInstance().AddIrCommand(new IrCommandLabel(endLabel));

        // Non-main functions (including class methods) end with jr $ra.
        // main() should fall through to the global exit sequence, so we do NOT emit jr $ra for it.
        if (isClassMethod || !"main".equals(this.name)) {
            Ir.getInstance().AddIrCommand(new IrCommandJrRa());
        }
        VarNameMapper.getInstance().endScope();

        return null;
    }
}
