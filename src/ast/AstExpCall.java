package ast;

import temp.*;
import ir.*;

public class AstExpCall extends AstExp
{
	/****************/
	/* DATA MEMBERS */
	/****************/
	public String funcName;
	public AstExpList params;

	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstExpCall(String funcName, AstExpList params)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();

		this.funcName = funcName;
		this.params = params;
	}

	/************************************************/
	/* The printing message for a call exp AST node */
	/************************************************/
	public void printMe()
	{
		/********************************/
		/* AST NODE TYPE = AST CALL EXP */
		/********************************/
		System.out.format("CALL(%s)\nWITH:\n",funcName);

		/***************************************/
		/* RECURSIVELY PRINT params + body ... */
		/***************************************/
		if (params != null) params.printMe();
		
		/***************************************/
		/* PRINT Node to AST GRAPHVIZ DOT file */
		/***************************************/
		AstGraphviz.getInstance().logNode(
                serialNumber,
			String.format("CALL(%s)\nWITH",funcName));
		
		/****************************************/
		/* PRINT Edges to AST GRAPHVIZ DOT file */
		/****************************************/
		AstGraphviz.getInstance().logEdge(serialNumber,params.serialNumber);
	}

	public Temp irMe()
	{
		Temp t = null;

		if (params != null) { t = params.head.irMe(); }

		Ir.getInstance().AddIrCommand(new IrCommandPrintInt(t));

		return null;
	}
}
