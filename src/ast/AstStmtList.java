package ast;

import types.*;
import temp.*;
import ir.*;

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
		/* AST NODE Type = AST STATEMENT LIST */
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
	/*****************/
	/* IR ME         */
	/*****************/
	public Temp irMe()
	{
		/**************************************/
		/* [1] Generate IR for the head       */
		/*     statement                      */
		/**************************************/
		if (head != null)
		{
			head.irMe();
		}

		/**************************************/
		/* [2] Recursively generate IR for    */
		/*     the tail (rest of statements)  */
		/**************************************/
		if (tail != null)
		{
			tail.irMe();
		}

		/**************************************/
		/* [3] Return null (statement lists   */
		/*     don't return values)           */
		/**************************************/
		return null;
	}
}
