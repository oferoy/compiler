package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstExpNewClass extends AstExp
{
    public String className;

    public AstExpNewClass(String className)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.className = className;
    }

    @Override
    public void printMe()
    {
        System.out.print("AST NODE NEW CLASS EXP\n");

        AstGraphviz.getInstance().logNode(
            serialNumber,
            String.format("NEW CLASS\n(%s)", className)
        );
    }

    @Override
    public Type semantMe()
    {
        // 1. Lookup type
        Type t = SymbolTable.getInstance().find(className);

        if (t == null) {
            AstNode.error(lineNumber,
                "undefined type: " + className);
        }

        // 2. Type must be a class
        if (!(t instanceof TypeClass)) {
            AstNode.error(lineNumber,
                "new operator requires a class type");
        }

        // 3. OK – return class type
        return t;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe() {
        Type t = SymbolTable.getInstance().find(className);
        if (!(t instanceof TypeClass))
            return null;
        TypeClass cls = (TypeClass) t;
        int numBytes = cls.getDataSize();
        Temp dst = TempFactory.getInstance().getFreshTemp();
        String vtableLabel = "vtable_" + cls.name;
        Ir.getInstance().AddIrCommand(new IrCommandAllocateClass(dst, numBytes, vtableLabel));
        // Initialize fields with default values (e.g. int age := 10). Walk inheritance chain (base first).
        java.util.ArrayList<types.TypeClass> chain = new java.util.ArrayList<>();
        for (types.TypeClass c = cls; c != null; c = c.father)
            chain.add(0, c);
        for (types.TypeClass c : chain) {
            for (types.TypeClassMember m = c.dataMembers; m != null; m = m.next) {
                if (m.kind == types.TypeClassMember.FIELD && m.initValue != null) {
                    AstExp initExp = (AstExp) m.initValue;
                    Temp initTemp = initExp.irMe();
                    int offset = cls.getFieldOffset(m.name);
                    Ir.getInstance().AddIrCommand(new IrCommandStoreField(dst, offset, initTemp));
                }
            }
        }
        return dst;
    }
}
