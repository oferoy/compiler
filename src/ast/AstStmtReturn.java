package ast;

import types.*;
import temp.*;
import ir.*;

public class AstStmtReturn extends AstStmt
{
    public AstExp exp;   // may be null for "return;"

    public AstStmtReturn(AstExp exp)
    {
        this.serialNumber = AstNodeSerialNumber.getFresh();
        this.exp = exp;
    }

    @Override
    public void printMe()
    {
        System.out.print("AST NODE RETURN STMT\n");
        if (exp != null) exp.printMe();

        AstGraphviz.getInstance().logNode(
            serialNumber,
            "RETURN");

        if (exp != null) {
            AstGraphviz.getInstance().logEdge(serialNumber, exp.serialNumber);
        }
    }

    @Override
    public Type semantMe()
    {
        // Must be inside a function
        if (AstDecFunc.currentReturnType == null) {
            AstNode.error(lineNumber, "return statement outside of function");
        }

        Type expected = AstDecFunc.currentReturnType;

        // Case 1: "return;" with no expression
        if (exp == null) {
            // Only legal for void functions
            if (!(expected instanceof TypeVoid)) {
                AstNode.error(lineNumber,
                    "missing return value in function " + AstDecFunc.currentFuncName);
            }
            return null;
        }

        // Case 2: "return exp;"
        Type actual = exp.semantMe();

        // Void expression is never allowed
        if (actual instanceof TypeVoid) {
            AstNode.error(lineNumber,
                "void expression cannot be returned");
        }

        // Must be assignable to the function's return type
        if (!TypesHelper.isAssignable(expected, actual)) {
            String expName = (actual == null ? "null" : actual.name);
            AstNode.error(lineNumber,
                "illegal return type: cannot return " + expName +
                " from function " + AstDecFunc.currentFuncName);
        }

        return null;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe()
    {
        Temp expTemp = null;
        if (exp != null)
            expTemp = exp.irMe();
        String endLabel = Ir.getCurrentFunctionEndLabel();
        Ir.getInstance().AddIrCommand(new IrCommandReturn(expTemp, endLabel));
        return null;
    }
}
