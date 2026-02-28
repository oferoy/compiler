package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstExpNewArray extends AstExp
{
    public AstTypeName typeName;   // The element type name
    public AstExp sizeExp;         // e in new T[e]

    public AstExpNewArray(AstTypeName typeName, AstExp sizeExp)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.typeName = typeName;
        this.sizeExp = sizeExp;
    }

    @Override
    public void printMe()
    {
        System.out.print("AST NODE NEW ARRAY EXP\n");

        if (typeName != null) typeName.printMe();
        if (sizeExp != null) sizeExp.printMe();

        AstGraphviz.getInstance().logNode(serialNumber, "NEW ARRAY");

        if (typeName != null) AstGraphviz.getInstance().logEdge(serialNumber, typeName.serialNumber);
        if (sizeExp != null) AstGraphviz.getInstance().logEdge(serialNumber, sizeExp.serialNumber);
    }

    @Override
    public Type semantMe()
    {
        // ------------------------------------------------------------
        // 1. Resolve the base type T (DO NOT call typeName.semantMe)
        // ------------------------------------------------------------
        Type elementType = SymbolTable.getInstance().find(typeName.type);

        if (elementType == null || elementType instanceof TypeVoid || elementType instanceof TypeNil)
        {
            AstNode.error(lineNumber,
                "illegal array element type: " + typeName.type);
        }

        // ------------------------------------------------------------
        // 2. Semant the size expression e
        // ------------------------------------------------------------
        Type sizeType = sizeExp.semantMe();

        if (!(sizeType instanceof TypeInt))
        {
            AstNode.error(lineNumber,
                "array size must be an integer expression");
        }

        // If literal, enforce > 0
        if (sizeExp instanceof AstExpInt)
        {
            int v = ((AstExpInt) sizeExp).value;
            if (v <= 0)
                AstNode.error(lineNumber, "array size must be > 0");
        }

        // ------------------------------------------------------------
        // 3. Return a fresh anonymous array type (unique ID)
        // ------------------------------------------------------------
        return new TypeArray(
            "anon_array_of_" + elementType.name,
            elementType
        );
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe() {
        Temp sizeTemp = sizeExp.irMe();
        Temp dst = TempFactory.getInstance().getFreshTemp();
        Ir.getInstance().AddIrCommand(new IrCommandAllocateArray(dst, sizeTemp));
        return dst;
    }
}
