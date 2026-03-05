package ast;

import types.*;
import symboltable.*;

public class AstTypeName extends AstNode
{
    public String type;  // the type name as string
    public String name;  // variable / field name
    /** Optional initial value for class fields (e.g. int age := 10). */
    public AstExp initValue;

    public AstTypeName(String type, String name)
    {
        serialNumber = AstNodeSerialNumber.getFresh();
        this.type = type;
        this.name = name;
    }

    @Override
    public void printMe()
    {
        System.out.format("NAME(%s):TYPE(%s)\n", name, type);

        AstGraphviz.getInstance().logNode(
                serialNumber,
                String.format("NAME:TYPE\n%s:%s", name, type));
    }

    @Override
    public Type semantMe()
    {
        Type t = SymbolTable.getInstance().find(type);
        if (t == null)
        {
            System.out.format(">> ERROR: undeclared type %s\n", type);
            System.exit(0);
        }

        SymbolTable.getInstance().enter(name, t);
        return t;
    }
}
