package ast;

import types.*;
import symboltable.*;

public class AstParamList extends AstNode
{
    public AstTypeName typeName;   // parameter type
    public String name;            // parameter name
    public AstParamList tail;      // next parameter

    // Constructor
    public AstParamList(AstTypeName typeName, String name, AstParamList tail)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.typeName = typeName;
        this.name = name;
        this.tail = tail;
    }

    // Print AST
    @Override
    public void printMe()
    {
        System.out.print("AST NODE PARAM LIST\n");

        if (typeName != null) typeName.printMe();
        if (tail != null) tail.printMe();

        AstGraphviz.getInstance().logNode(
            serialNumber,
            String.format("PARAM\n(%s)", name)
        );

        if (typeName != null) AstGraphviz.getInstance().logEdge(serialNumber, typeName.serialNumber);
        if (tail != null) AstGraphviz.getInstance().logEdge(serialNumber, tail.serialNumber);
    }

    // Helper to check duplicate parameter names
    private void checkDuplicateNames()
    {
        AstParamList iter = tail;
        while (iter != null)
        {
            if (iter.name.equals(this.name))
            {
                AstNode.error(this.lineNumber, "duplicate parameter name: " + name);
            }
            iter = iter.tail;
        }
    }

    // Semantic analysis of parameter list
    @Override
    public Type semantMe()
    {
        // check Type correctness
        Type paramType = typeName.semantMe();

        if (paramType instanceof TypeVoid)
        {
            AstNode.error(this.lineNumber, "parameter cannot be of Type void");
        }

        // check for duplicates further down the list
        checkDuplicateNames();

        // semant tail if exists
        if (tail != null)
        {
            tail.semantMe();
        }

        return null;
    }

    // Build TypeList for function signature
    public TypeList buildTypeList() throws Exception
    {
        Type headType = typeName.semantMe();

        if (tail == null)
        {
            return new TypeList(headType, null);
        }

        return new TypeList(headType, tail.buildTypeList());
    }

    // Insert parameters into the current scope (function scope)
    public void insertParamsIntoScope() throws Exception
    {
        Type paramType = typeName.semantMe();

        // cannot shadow something already declared in function scope
        if (SymbolTable.findInCurrentScope(name) != null)
        {
            AstNode.error(this.lineNumber, "parameter shadows existing name: " + name);
        }

        TypeVar paramEntry = new TypeVar(paramType, name);

        if (!SymbolTable.insert(name, paramEntry))
        {
            AstNode.error(this.lineNumber, "failed inserting parameter: " + name);
        }

        if (tail != null)
        {
            tail.insertParamsIntoScope();
        }
    }
}