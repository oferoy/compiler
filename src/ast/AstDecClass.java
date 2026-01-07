package ast;

import types.*;
import symboltable.*;

public class AstDecClass extends AstDec
{
	/********/
	/* NAME */
	/********/
	public String name;

	/****************/
	/* DATA MEMBERS */
	/****************/
	public AstTypeNameList dataMembers;
	
	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstDecClass(String name, AstTypeNameList dataMembers)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();
	
		this.name = name;
		this.dataMembers = dataMembers;
	}

	/*********************************************************/
	/* The printing message for a class declaration AST node */
	/*********************************************************/
	public void printMe()
	{
		/*************************************/
		/* RECURSIVELY PRINT HEAD + TAIL ... */
		/*************************************/
		System.out.format("CLASS DEC = %s\n",name);
		if (dataMembers != null) dataMembers.printMe();
		
		/***************************************/
		/* PRINT Node to AST GRAPHVIZ DOT file */
		/***************************************/
		AstGraphviz.getInstance().logNode(
                serialNumber,
			String.format("CLASS\n%s",name));
		
		/****************************************/
		/* PRINT Edges to AST GRAPHVIZ DOT file */
		/****************************************/
		AstGraphviz.getInstance().logEdge(serialNumber, dataMembers.serialNumber);
	}
	
	public Type semantMe()
	{	
		/*************************/
		/* [1] Begin Class Scope */
		/*************************/
		SymbolTable.getInstance().beginScope();

		/***************************/
		/* [2] Semant Data Members */
		/***************************/
		TypeClass t = new TypeClass(null,name, dataMembers.semantMe());

		/*****************/
		/* [3] End Scope */
		/*****************/
		SymbolTable.getInstance().endScope();

		/************************************************/
		/* [4] Enter the Class Type to the Symbol Table */
		/************************************************/
		SymbolTable.getInstance().enter(name,t);

		/*********************************************************/
		/* [5] Return value is irrelevant for class declarations */
		/*********************************************************/
		return null;		
	}
}
