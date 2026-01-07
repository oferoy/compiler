/***********/
/* PACKAGE */
/***********/
package ast;

/*******************/
/* PROJECT IMPORTS */
/*******************/
import types.*;
import symboltable.*;

public class AstTypeName extends AstNode
{
	/****************/
	/* DATA MEMBERS */
	/****************/
	public String type;
	public String name;
	
	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstTypeName(String type, String name)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();
	
		this.type = type;
		this.name = name;
	}

	/*************************************************/
	/* The printing message for a type name AST node */
	/*************************************************/
	public void printMe()
	{
		/**************************************/
		/* AST NODE TYPE = AST TYPE NAME NODE */
		/**************************************/
		System.out.format("NAME(%s):TYPE(%s)\n",name,type);

		/***************************************/
		/* PRINT Node to AST GRAPHVIZ DOT file */
		/***************************************/
		AstGraphviz.getInstance().logNode(
				serialNumber,
			String.format("NAME:TYPE\n%s:%s",name,type));
	}

	public Type semantMe()
	{
		Type t = SymbolTable.getInstance().find(type);
		if (t == null)
		{
			/**************************/
			/* ERROR: undeclared type */
			/**************************/
			System.exit(0);
			return null;
		}
		else
		{
			/*******************************************************/
			/* Enter var with name=name and type=t to symbol table */
			/*******************************************************/
			SymbolTable.getInstance().enter(name,t);
		}

		/****************************/
		/* return (existing) type t */
		/****************************/
		return t;
	}	
}
