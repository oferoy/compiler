package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstExpVarSimple extends AstExpVar
{
    public String name;

    public AstExpVarSimple(String name)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.name = name;
    }

    @Override
    public void printMe()
    {
        System.out.format("VAR_SIMPLE(%s)\n", name);

        AstGraphviz.getInstance().logNode(
            serialNumber,
            String.format("VAR_SIMPLE\n(%s)", name));
    }

    @Override
    public Type semantMe()
    {
        // 1. Normal lookup in symbol table (locals, params, globals, etc.)
        Type t = SymbolTable.getInstance().find(name);

        /***************************************************/
        /* 2. If this name is a local/parameter of         */
        /*    the CURRENT FUNCTION, always use that.       */
        /*    (locals/params shadow fields & globals)      */
        /***************************************************/
        if (AstDecFunc.isCurrentLocalOrParam(name)) {
            if (t == null) {
                AstNode.error(lineNumber,
                    "undefined variable: " + name);
            }
            return t;
        }

        /***************************************************/
        /* 3. Not a local/param. If we are inside a class, */
        /*    try to resolve as a FIELD first.              */
        /***************************************************/
        if (AstDecClass.currentClassType != null) {
            TypeClass cls = AstDecClass.currentClassType;

            Type fieldType =
                TypesHelper.findVisibleField(cls, name, lineNumber);

            if (fieldType != null) {
                // Either a field of this class (declared before use),
                // or an inherited field -> field shadows globals.
                return fieldType;
            }
        }

        /***************************************************/
        /* 4. No local/param, no visible field -> fall     */
        /*    back to symbol table (globals, functions,    */
        /*    typedefs, etc.).                             */
        /***************************************************/
        if (t == null) {
            AstNode.error(lineNumber,
                "undefined variable: " + name);
        }

        return t;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe()
    {
        Temp t = TempFactory.getInstance().getFreshTemp();

        // In a class method, a bare identifier can be a field (e.g. "grades" -> this.grades). Load this + loadField.
        if (AstDecClass.currentClassType != null && !AstDecFunc.isCurrentLocalOrParam(name)) {
            Type fieldType = TypesHelper.findVisibleField(AstDecClass.currentClassType, name, lineNumber);
            if (fieldType != null) {
                String thisIrName = VarNameMapper.getInstance().getIrName("this");
                if (thisIrName != null) {
                    Temp baseTemp = TempFactory.getInstance().getFreshTemp();
                    Ir.getInstance().AddIrCommand(new IrCommandLoad(baseTemp, thisIrName));
                    int offset = AstDecClass.currentClassType.getFieldOffset(name);
                    Ir.getInstance().AddIrCommand(new IrCommandLoadField(t, baseTemp, offset));
                    return t;
                }
            }
        }

        String irName = VarNameMapper.getInstance().getIrName(name);
        if (irName == null)
            irName = name;
        Ir.getInstance().AddIrCommand(new IrCommandLoad(t, irName));
        return t;
    }
}
