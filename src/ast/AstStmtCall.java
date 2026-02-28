package ast;

import types.*;
import temp.*;
import ir.*;

public class AstStmtCall extends AstStmt
{
    /****************/
    /* DATA MEMBERS */
    /****************/
    public AstExpCall callExp;
    
    /******************/
    /* CONSTRUCTOR(S) */
    /******************/
    public AstStmtCall(AstExpCall callExp)
    {
        /******************************/
        /* SET A UNIQUE SERIAL NUMBER */
        /******************************/
        serialNumber = AstNodeSerialNumber.getFresh();

        this.callExp = callExp;
    }
    
    public void printMe()
    {
        callExp.printMe();

        /***************************************/
        /* PRINT Node to AST GRAPHVIZ DOT file */
        /***************************************/
        AstGraphviz.getInstance().logNode(
            serialNumber,
            String.format("STMT\nCALL"));
        
        /****************************************/
        /* PRINT Edges to AST GRAPHVIZ DOT file */
        /****************************************/
        AstGraphviz.getInstance().logEdge(serialNumber, callExp.serialNumber);
    }

    @Override
    public Type semantMe()
    {
        if (callExp != null) {
            // Statement context: void result is allowed
            callExp.semantMeAsStmt();
        }
        return null;
    }

    /*****************/
    /* IR ME         */
    /*****************/
    @Override
    public Temp irMe()
    {
        /**************************************/
        /* Delegate to the call expression's  */
        /* irMe() - it handles PrintInt       */
        /**************************************/
        if (callExp != null)
        {
            callExp.irMe();
        }
        
        return null;
    }
}
