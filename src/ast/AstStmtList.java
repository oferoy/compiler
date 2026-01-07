package ast;

import types.*;
import temp.*;

public class AstStmtList extends AstNode
{
	/****************/
	/* DATA MEMBERS */
	/****************/
	public AstStmt head;
	public AstStmtList tail;

	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstStmtList(AstStmt head, AstStmtList tail)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();

		/***************************************/
		/* PRINT CORRESPONDING DERIVATION RULE */
		/***************************************/
		if (tail != null) System.out.print("====================== stmts -> stmt stmts\n");
		if (tail == null) System.out.print("====================== stmts -> stmt      \n");

		/*******************************/
		/* COPY INPUT DATA MEMBERS ... */
		/*******************************/
		this.head = head;
		this.tail = tail;
	}

	/******************************************************/
	/* The printing message for a statement list AST node */
	/******************************************************/
	public void printMe()
	{
		/**************************************/
		/* AST NODE TYPE = AST STATEMENT LIST */
		/**************************************/
		System.out.print("AST NODE STMT LIST\n");

		/*************************************/
		/* RECURSIVELY PRINT HEAD + TAIL ... */
		/*************************************/
		if (head != null) head.printMe();
		if (tail != null) tail.printMe();

		/**********************************/
		/* PRINT to AST GRAPHVIZ DOT file */
		/**********************************/
		AstGraphviz.getInstance().logNode(
				serialNumber,
			"STMT\nLIST\n");
		
		/****************************************/
		/* PRINT Edges to AST GRAPHVIZ DOT file */
		/****************************************/
		if (head != null) AstGraphviz.getInstance().logEdge(serialNumber,head.serialNumber);
		if (tail != null) AstGraphviz.getInstance().logEdge(serialNumber,tail.serialNumber);
	}
	
	public Type semantMe()
	{
		if (head != null) head.semantMe();
		if (tail != null) tail.semantMe();
		
		return null;
	}

	public Temp irMe()
	{
		if (head != null) head.irMe();
		if (tail != null) tail.irMe();

		return null;
	}
}
