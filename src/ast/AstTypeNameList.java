package ast;

import types.*;

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
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();

		this.head = head;
		this.tail = tail;
	}

	/******************************************************/
	/* The printing message for a type name list AST node */
	/******************************************************/
	public void printMe()
	{
		/**************************************/
		/* AST NODE TYPE = AST TYPE NAME LIST */
		/**************************************/
		System.out.print("AST TYPE NAME LIST\n");

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
			"TYPE-NAME\nLIST\n");
		
		/****************************************/
		/* PRINT Edges to AST GRAPHVIZ DOT file */
		/****************************************/
		if (head != null) AstGraphviz.getInstance().logEdge(serialNumber,head.serialNumber);
		if (tail != null) AstGraphviz.getInstance().logEdge(serialNumber,tail.serialNumber);
	}

	public TypeList semantMe()
	{
		if (tail == null)
		{
			return new TypeList(
				head.semantMe(),
				null);
		}
		else
		{
			return new TypeList(
				head.semantMe(),
				tail.semantMe());
		}
	}
}
