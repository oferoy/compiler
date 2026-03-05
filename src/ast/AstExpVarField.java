package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstExpVarField extends AstExpVar
{
    public AstExpVar var;
    public String fieldName;
    /** Byte offset of field in class layout (set in semantMe). */
    public int fieldOffset;

    public AstExpVarField(AstExpVar var, String fieldName)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.var = var;
        this.fieldName = fieldName;
    }

    @Override
    public void printMe()
    {
        System.out.format("FIELD VAR (%s)\n", fieldName);

        if (var != null) var.printMe();

        AstGraphviz.getInstance().logNode(
            serialNumber,
            String.format("FIELD\n___.%s", fieldName)
        );

        if (var != null)
            AstGraphviz.getInstance().logEdge(serialNumber, var.serialNumber);
    }

    @Override
    public Type semantMe()
    {
        // ------------------------------------------------------------
        // 1. Semant the base (object)
        // ------------------------------------------------------------
        Type baseType = var.semantMe();

        if (!(baseType instanceof TypeClass)) {
            AstNode.error(lineNumber,
                "accessing field " + fieldName +
                " of non-class type " + baseType.name);
        }

        TypeClass cls = (TypeClass) baseType;

        // ------------------------------------------------------------
        // 2. Look for the field and compute offset
        // ------------------------------------------------------------
        Type fieldType = cls.findField(fieldName);

        if (fieldType == null) {
            AstNode.error(lineNumber,
                "field " + fieldName +
                " does not exist in class " + cls.name);
        }

        // ------------------------------------------------------------
        // 3. Ensure it's a field, not a method
        // ------------------------------------------------------------
        if (fieldType instanceof TypeFunction) {
            AstNode.error(lineNumber,
                "method " + fieldName +
                " cannot be accessed without a call");
        }

        this.fieldOffset = cls.getFieldOffset(fieldName);
        return fieldType;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe() {
        Temp baseTemp = var.irMe();
        Temp dst = TempFactory.getInstance().getFreshTemp();
        Ir.getInstance().AddIrCommand(new IrCommandLoadField(dst, baseTemp, fieldOffset));
        return dst;
    }
}
