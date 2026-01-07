package ast;

import types.*;
import temp.*;
import ir.*;

public class AstDecList extends AstNode
{
	/****************/
	/* DATA MEMBERS */
	/****************/
	public AstDec head;
	public AstDecList tail;

	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstDecList(AstDec head, AstDecList tail)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();

		this.head = head;
		this.tail = tail;
	}

	/********************************************************/
	/* The printing message for a declaration list AST node */
	/********************************************************/
	public void printMe()
	{
		/********************************/
		/* AST NODE TYPE = AST DEC LIST */
		/********************************/
		System.out.print("AST NODE DEC LIST\n");

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
			"DEC\nLIST\n");
				
		/****************************************/
		/* PRINT Edges to AST GRAPHVIZ DOT file */
		/****************************************/
		if (head != null) AstGraphviz.getInstance().logEdge(serialNumber,head.serialNumber);
		if (tail != null) AstGraphviz.getInstance().logEdge(serialNumber,tail.serialNumber);
	}

	public Type semantMe()
	{
		/*************************************/
		/* RECURSIVELY PRINT HEAD + TAIL ... */
		/*************************************/
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
