package ast;

import types.*;

public class AstExpList extends AstNode
{
    public AstExp head;
    public AstExpList tail;

    public AstExpList(AstExp head, AstExpList tail)
    {
        serialNumber = AstNodeSerialNumber.getFresh();
        this.head = head;
        this.tail = tail;
    }

    @Override
    public void printMe()
    {
        System.out.print("AST NODE EXP LIST\n");

        if (head != null) head.printMe();
        if (tail != null) tail.printMe();

        AstGraphviz.getInstance().logNode(
                serialNumber,
                "EXP\nLIST");

        if (head != null)
            AstGraphviz.getInstance().logEdge(serialNumber, head.serialNumber);

        if (tail != null)
            AstGraphviz.getInstance().logEdge(serialNumber, tail.serialNumber);
    }

    @Override
    public Type semantMe()
    {
        // Semant each expression in the list
        if (head != null) head.semantMe();
        if (tail != null) tail.semantMe();

        return null; // lists don't produce a type
    }
}
