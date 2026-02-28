package ast;

import types.*;
import temp.*;
import ir.*;

public class AstExpString extends AstExp
{
    public String value;

    /******************/
    /* ORIGINAL CONSTRUCTOR USED BY CUP */
    /******************/
    public AstExpString(String value)
    {
        this(value, 0); // default line number
    }

    /******************/
    /* NEW CONSTRUCTOR WITH LINE NUMBER */
    /******************/
    public AstExpString(String value, int lineNumber)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.value = value;
        this.lineNumber = lineNumber;
    }

    @Override
    public void printMe()
    {
        System.out.format("AST NODE STRING( %s )\n", value);

        AstGraphviz.getInstance().logNode(
            serialNumber,
            String.format("STRING\n%s", value.replace('"','\''))
        );
    }

    @Override
    public Type semantMe()
    {
        return TypeString.getInstance();
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe() {
        String label = "str_" + serialNumber;
        Ir.getInstance().AddIrCommand(new IrCommandAllocateString(label, value));
        Temp dst = TempFactory.getInstance().getFreshTemp();
        Ir.getInstance().AddIrCommand(new IrCommandLoadAddress(dst, label));
        return dst;
    }
}
