package ast;

import temp.*;

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
		AstGraphviz.getInstance().logEdge(serialNumber,callExp.serialNumber);
	}

	public Temp irMe()
	{
		if (callExp != null) callExp.irMe();

		return null;
	}
}