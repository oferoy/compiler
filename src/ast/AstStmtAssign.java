package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstStmtAssign extends AstStmt
{
    /****************/
    /* DATA MEMBERS */
    /****************/
    public AstExpVar var;
    public AstExp    exp;

    /******************/
    /* CONSTRUCTOR(S) */
    /******************/
    public AstStmtAssign(AstExpVar var, AstExp exp)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.var = var;
        this.exp = exp;
    }

    /***************/
    /* PRINT ME    */
    /***************/
    @Override
    public void printMe()
    {
        System.out.print("AST NODE ASSIGN STMT\n");

        if (var != null)  var.printMe();
        if (exp != null)  exp.printMe();

        AstGraphviz.getInstance().logNode(
            serialNumber,
            "ASSIGN");

        if (var != null)
            AstGraphviz.getInstance().logEdge(serialNumber, var.serialNumber);
        if (exp != null)
            AstGraphviz.getInstance().logEdge(serialNumber, exp.serialNumber);
    }

    /*****************/
    /* SEMANT ME     */
    /*****************/
    @Override
    public Type semantMe()
    {
        // 1. Get the type of the left-hand side (variable)
        Type dst = null;
        if (var != null) {
            dst = var.semantMe();
        }

        // 2. Get the type of the right-hand side (expression)
        Type src = null;
        if (exp != null) {
            src = exp.semantMe();
        }

        // Safety: if either side failed for some reason, bail
        if (dst == null || src == null) {
            AstNode.error(lineNumber, "null type in assignment");
        }

        // 3. Void expression is NEVER a legal rvalue in assignment
        if (src instanceof TypeVoid) {
            AstNode.error(lineNumber,
                "cannot assign value of type void");
        }

        // 4. Use the global assignment rules
        if (!TypesHelper.isAssignable(dst, src)) {
            String dstName = (dst == null ? "null" : dst.name);
            String srcName = (src == null ? "null" : src.name);
            AstNode.error(lineNumber,
                "cannot assign " + srcName + " to variable of type " + dstName);
        }

        return null;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe()
    {
        // Spec: left-hand side evaluated first (then RHS).
        if (var instanceof AstExpVarSimple)
        {
            Temp rhsTemp = exp.irMe();
            String originalName = ((AstExpVarSimple) var).name;
            // In a class method, bare identifier can be a field (e.g. age := age+1) -> must store to object, not a local
            if (AstDecClass.currentClassType != null && !AstDecFunc.isCurrentLocalOrParam(originalName)) {
                Type fieldType = TypesHelper.findVisibleField(AstDecClass.currentClassType, originalName, var.lineNumber);
                if (fieldType != null) {
                    String thisIrName = VarNameMapper.getInstance().getIrName("this");
                    if (thisIrName != null) {
                        Temp baseTemp = TempFactory.getInstance().getFreshTemp();
                        Ir.getInstance().AddIrCommand(new IrCommandLoad(baseTemp, thisIrName));
                        int offset = AstDecClass.currentClassType.getFieldOffset(originalName);
                        Ir.getInstance().AddIrCommand(new IrCommandStoreField(baseTemp, offset, rhsTemp));
                        return null;
                    }
                }
            }
            String irName = VarNameMapper.getInstance().getIrName(originalName);
            if (irName == null)
                irName = originalName;
            Ir.getInstance().AddIrCommand(new IrCommandStore(irName, rhsTemp));
        }
        else if (var instanceof AstExpVarField)
        {
            AstExpVarField f = (AstExpVarField) var;
            if (exp instanceof AstExpCall)
            {
                // When base is simple var (e.g. l3), load it directly into $s2 from memory so no temp can be clobbered.
                String baseIrName = null;
                if (f.var instanceof AstExpVarSimple) {
                    baseIrName = VarNameMapper.getInstance().getIrName(((AstExpVarSimple) f.var).name);
                    if (baseIrName == null) baseIrName = ((AstExpVarSimple) f.var).name;
                    Ir.getInstance().AddIrCommand(new IrCommandLoadVarToS2(baseIrName));
                } else {
                    Temp baseTemp = f.var.irMe();
                    Ir.getInstance().AddIrCommand(new IrCommandMoveBaseToS2(baseTemp));
                }
                AstExpCall call = (AstExpCall) exp;
                java.util.List<Temp> argTemps = new java.util.ArrayList<>();
                for (AstExpList e = call.params; e != null; e = e.tail)
                    if (e.head != null) argTemps.add(e.head.irMe());
                java.util.Collections.reverse(argTemps);
                boolean isVirtual = (call.isMethodCall && call.firstArgType instanceof TypeClass);
                int methodSlot = isVirtual ? types.VtableBuilder.getMethodSlot(call.funcName) : -1;
                String mipsLabel = call.funcName;
                if (isVirtual && methodSlot < 0) {
                    TypeClass definingClass = ((TypeClass) call.firstArgType).getMethodDefiningClass(call.funcName);
                    if (definingClass != null)
                        mipsLabel = definingClass.name + "_" + call.funcName;
                }
                Ir.getInstance().AddIrCommand(new IrCommandCallAndStoreFieldWithBaseInS2(mipsLabel, argTemps, f.fieldOffset, isVirtual && methodSlot >= 0, methodSlot));
                // Restore base var from $s2 so outer activation's slot is correct for later "return l3" (callee may have overwritten it).
                if (baseIrName != null)
                    Ir.getInstance().AddIrCommand(new IrCommandStoreS2ToVar(baseIrName));
            }
            else
            {
                Temp baseTemp = f.var.irMe();
                Temp rhsTemp = exp.irMe();
                Ir.getInstance().AddIrCommand(new IrCommandStoreField(baseTemp, f.fieldOffset, rhsTemp));
            }
        }
        else if (var instanceof AstExpVarSubscript)
        {
            AstExpVarSubscript s = (AstExpVarSubscript) var;
            // LHS must be evaluated before RHS: e.g. arr[age] := arr[birthday()]+1000
            // must use age (10) before birthday() runs and updates it to 11.
            // Use $s1 for index: move to $s1, push, RHS, pop, then store (index=null uses $s1).
            Temp indexTemp = s.subscript.irMe();
            Ir.getInstance().AddIrCommand(new IrCommandMoveToS1(indexTemp));
            Ir.getInstance().AddIrCommand(new IrCommandPushS1());
            Temp arrayBase = s.var.irMe();
            Ir.getInstance().AddIrCommand(new IrCommandMoveBaseToS2(arrayBase));
            Ir.getInstance().AddIrCommand(new IrCommandPushS2());
            Temp rhsTemp = exp.irMe();
            Ir.getInstance().AddIrCommand(new IrCommandPopS2());
            Ir.getInstance().AddIrCommand(new IrCommandPopS1());
            Ir.getInstance().AddIrCommand(new IrCommandStoreArrayWithBaseInS2(null, rhsTemp));
        }

        return null;
    }
}
