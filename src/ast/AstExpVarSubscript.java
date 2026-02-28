package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstExpVarSubscript extends AstExpVar
{
    public AstExpVar var;
    public AstExp subscript;

    public AstExpVarSubscript(AstExpVar var, AstExp subscript)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.var = var;
        this.subscript = subscript;
    }

    @Override
    public void printMe()
    {
        System.out.print("AST NODE SUBSCRIPT VAR\n");

        if (var != null) var.printMe();
        if (subscript != null) subscript.printMe();

        AstGraphviz.getInstance().logNode(serialNumber, "VAR[ ]");

        if (var != null)
            AstGraphviz.getInstance().logEdge(serialNumber, var.serialNumber);
        if (subscript != null)
            AstGraphviz.getInstance().logEdge(serialNumber, subscript.serialNumber);
    }

    @Override
    public Type semantMe()
    {
        // ------------------------------------------------------------
        // 1. Semant the base: must be an array
        // ------------------------------------------------------------
        Type base = var.semantMe();

        if (!(base instanceof TypeArray)) {
            AstNode.error(lineNumber,
                "subscript applied to non-array variable of type " + base.name);
        }

        TypeArray arr = (TypeArray) base;

        // ------------------------------------------------------------
        // 2. Semant the index: must be int
        // ------------------------------------------------------------
        Type idxType = subscript.semantMe();

        if (!(idxType instanceof TypeInt)) {
            AstNode.error(lineNumber,
                "array subscript must be int, got " + idxType.name);
        }

        // (Optional) static check for negative literal indexes
        if (subscript instanceof AstExpInt) {
            int v = ((AstExpInt) subscript).value;
            if (v < 0) {
                AstNode.error(lineNumber, "array index must be >= 0");
            }
        }

        // ------------------------------------------------------------
        // 3. Resulting type is the element type of the array
        // ------------------------------------------------------------
        return arr.elementType;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe() {
        // Evaluate index first so any call in subscript does not clobber the array base.
        // When subscript is a call (e.g. arr[birthday()]), push index so loading base cannot overwrite it.
        Temp indexTemp = subscript.irMe();
        if (subscript instanceof AstExpCall) {
            Ir.getInstance().AddIrCommand(new IrCommandPush(indexTemp));
            Temp arrayBase = var.irMe();
            Temp indexRestored = TempFactory.getInstance().getFreshTemp();
            Ir.getInstance().AddIrCommand(new IrCommandPop(indexRestored));
            Temp dst = TempFactory.getInstance().getFreshTemp();
            Ir.getInstance().AddIrCommand(new IrCommandLoadArray(dst, arrayBase, indexRestored));
            return dst;
        }
        Temp arrayBase = var.irMe();
        Temp dst = TempFactory.getInstance().getFreshTemp();
        Ir.getInstance().AddIrCommand(new IrCommandLoadArray(dst, arrayBase, indexTemp));
        return dst;
    }
}
