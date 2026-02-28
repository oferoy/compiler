package ast;

import types.*;
import symboltable.*;

public class AstTypeNameList extends AstNode
{
    /****************/
    /* DATA MEMBERS */
    /****************/
    public AstTypeName head;
    public AstTypeNameList tail;

    /******************/
    /* CONSTRUCTOR(S) */
    /******************/
    public AstTypeNameList(AstTypeName head, AstTypeNameList tail)
    {
        serialNumber = AstNodeSerialNumber.getFresh();
        this.head = head;
        this.tail = tail;
    }

    /******************************************************/
    /* The printing message for a Type name list AST node */
    /******************************************************/
    public void printMe()
    {
        System.out.print("AST Type NAME LIST\n");

        if (head != null) head.printMe();
        if (tail != null) tail.printMe();

        AstGraphviz.getInstance().logNode(
            serialNumber,
            "TYPE-NAME\nLIST\n");

        if (head != null) AstGraphviz.getInstance().logEdge(serialNumber, head.serialNumber);
        if (tail != null) AstGraphviz.getInstance().logEdge(serialNumber, tail.serialNumber);
    }

    /******************************************************/
    /* Build a TypeList for the formal parameters         */
    /******************************************************/
    public TypeList buildTypeList() {
        if (head == null) {
            return null;
        }

        Type headType;

        if ("int".equals(head.type)) {
            headType = TypeInt.getInstance();
        } else if ("string".equals(head.type)) {
            headType = TypeString.getInstance();
        } else if ("void".equals(head.type)) {
            AstNode.error(lineNumber, "parameter type cannot be void");
            headType = null; // unreachable
        } else {
            headType = SymbolTable.find(head.type);
            if (headType == null) {
                AstNode.error(lineNumber,
                    "non-existing parameter type: " + head.type);
            }
        }

        TypeList tailTypes = null;
        if (tail != null) {
            tailTypes = tail.buildTypeList();
        }

        return new TypeList(headType, tailTypes);
    }

    /******************************************************/
    /* Insert parameters as variables into current scope  */
    /******************************************************/
    public void insertParamsIntoScope() {
        if (head == null) return;

        Type paramType;

        if ("int".equals(head.type)) {
            paramType = TypeInt.getInstance();
        } else if ("string".equals(head.type)) {
            paramType = TypeString.getInstance();
        } else if ("void".equals(head.type)) {
            AstNode.error(lineNumber, "parameter type cannot be void");
            paramType = null; // unreachable
        } else {
            paramType = SymbolTable.find(head.type);
            if (paramType == null) {
                AstNode.error(lineNumber,
                    "non-existing parameter type: " + head.type);
            }
        }

        // Check duplicate name in current function scope
        if (!SymbolTable.insert(head.name, paramType)) {
            AstNode.error(lineNumber,
                "duplicate parameter name: " + head.name);
        }

        if (tail != null) {
            tail.insertParamsIntoScope();
        }
    }

    @Override
    public Type semantMe() {
        // Not used directly; we provide buildTypeList/insertParamsInstead.
        // Return value unused in this exercise.
        buildTypeList();
        return null;
    }
}
