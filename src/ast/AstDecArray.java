package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstDecArray extends AstDec
{
    public String name;          // array name
    public AstTypeName element;  // element type (as string)

    public AstDecArray(String name, AstTypeName element)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.name = name;
        this.element = element;
    }

    @Override
    public void printMe()
    {
        System.out.print("AST NODE ARRAY DECL\n");

        if (element != null) element.printMe();

        AstGraphviz.getInstance().logNode(
            serialNumber,
            String.format("ARRAY\n(%s)", name)
        );

        if (element != null)
            AstGraphviz.getInstance().logEdge(serialNumber, element.serialNumber);
    }

    @Override
    public Type semantMe()
    {
        // 1. Ensure array name not already defined
        if (SymbolTable.getInstance().find(name) != null)
        {
            AstNode.error(lineNumber,
                "array type " + name + " already declared");
        }

        // 2. Resolve the element type (DO NOT call element.semantMe)
        Type elementType = SymbolTable.getInstance().find(element.type);
        if (elementType == null || elementType instanceof TypeVoid || elementType instanceof TypeNil)
        {
            AstNode.error(lineNumber,
                "illegal array element type: " + element.type);
        }

        // 3. Create distinct array type
        TypeArray arrayType = new TypeArray(name, elementType);

        // 4. Insert into symbol table
        SymbolTable.getInstance().enter(name, arrayType);

        return null;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe() {
        return null;  // type declarations don't generate IR
    }
}
