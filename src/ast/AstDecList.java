package ast;

import types.Type;
import temp.*;
import ir.*;

public class AstDecList extends AstNode {

    public AstDec head;
    public AstDecList tail;

    public AstDecList(AstDec head, AstDecList tail) {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.head = head;
        this.tail = tail;
    }

    @Override
    public void printMe()
    {
        System.out.print("AST NODE DEC LIST\n");

        if (head != null) head.printMe();
        if (tail != null) tail.printMe();

        AstGraphviz.getInstance().logNode(
            serialNumber,
            "DEC\nLIST\n");

        if (head != null) AstGraphviz.getInstance().logEdge(serialNumber, head.serialNumber);
        if (tail != null) AstGraphviz.getInstance().logEdge(serialNumber, tail.serialNumber);
    }

    @Override
    public Type semantMe() {
        if (head != null) {
            head.semantMe();
        }
        if (tail != null) {
            tail.semantMe();
        }
        return null;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    public Temp irMe()
    {
        /**************************************/
        /* [1] Generate IR for the head       */
        /*     declaration                    */
        /**************************************/
        if (head != null)
        {
            head.irMe();
        }

        /**************************************/
        /* [2] Recursively generate IR for    */
        /*     the tail (rest of declarations)*/
        /**************************************/
        if (tail != null)
        {
            tail.irMe();
        }

        /**************************************/
        /* [3] Return null (declaration lists */
        /*     don't return values)           */
        /**************************************/
        return null;
    }
}
