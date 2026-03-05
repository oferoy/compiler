package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstStmtWhile extends AstStmt
{
    public AstExp cond;
    public AstStmtList body;

    /*******************/
    /*  CONSTRUCTOR(S) */
    /*******************/
    public AstStmtWhile(AstExp cond, AstStmtList body)
    {
        serialNumber = AstNodeSerialNumber.getFresh();
        this.cond = cond;
        this.body = body;
    }

    @Override
    public void printMe()
    {
        System.out.print("AST NODE STMT WHILE\n");

        if (cond != null) cond.printMe();
        if (body != null) body.printMe();

        AstGraphviz.getInstance().logNode(
                serialNumber,
                "WHILE (cond)\nDO body");

        if (cond != null) AstGraphviz.getInstance().logEdge(serialNumber, cond.serialNumber);
        if (body != null) AstGraphviz.getInstance().logEdge(serialNumber, body.serialNumber);
    }

    @Override
    public Type semantMe()
    {
        Type condType = cond.semantMe();

        if (!(condType instanceof TypeInt)) {
            AstNode.error(lineNumber, "while condition must be int");
        }

        // Open a new scope for the WHILE body
        SymbolTable.beginScope();
        
        body.semantMe();
        
        // Close the WHILE body scope
        SymbolTable.endScope();
        
        return null;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe()
    {
        /*******************************/
        /* [1] Allocate 2 fresh labels */
        /*******************************/
        String labelStart = IrCommand.getFreshLabel("while_start");
        String labelEnd   = IrCommand.getFreshLabel("while_end");

        /*********************************/
        /* [2] Entry label for the while */
        /*********************************/
        Ir.getInstance().AddIrCommand(new IrCommandLabel(labelStart));

        /********************/
        /* [3] cond.IRme(); */
        /********************/
        Temp condTemp = cond.irMe();

        /******************************************/
        /* [4] Jump conditionally to the loop end */
        /******************************************/
        Ir.getInstance().AddIrCommand(new IrCommandJumpIfEqToZero(condTemp, labelEnd));

        /*******************/
        /* [5] body.IRme() */
        /*******************/
        if (body != null)
        {
            // Begin scope for VarNameMapper (to handle shadowing)
            VarNameMapper.getInstance().beginScope();
            
            body.irMe();
            
            // End scope for VarNameMapper
            VarNameMapper.getInstance().endScope();
        }

        /******************************/
        /* [6] Jump to the loop entry */
        /******************************/
        Ir.getInstance().AddIrCommand(new IrCommandJumpLabel(labelStart));

        /**********************/
        /* [7] Loop end label */
        /**********************/
        Ir.getInstance().AddIrCommand(new IrCommandLabel(labelEnd));

        /*******************/
        /* [8] Return null */
        /*******************/
        return null;
    }
}
