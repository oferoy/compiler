package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstExpNil extends AstExp
{
    public AstExpNil()
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
    }

    @Override
    public void printMe()
    {
        System.out.print("AST NODE NIL\n");
        AstGraphviz.getInstance().logNode(serialNumber, "NIL");
    }

    @Override
    public Type semantMe()
    {
        // 'nil' has type TypeNil singleton
        return TypeNil.getInstance(); // or whatever your TypeNil singleton access is
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe() {
        Temp t = TempFactory.getInstance().getFreshTemp();
        Ir.getInstance().AddIrCommand(new IRcommandConstInt(t, 0));
        return t;
    }
}
