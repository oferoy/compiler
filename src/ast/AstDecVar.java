package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;


public class AstDecVar extends AstDec
{
	/****************/
	/* DATA MEMBERS */
	/****************/
	public String type;
	public String name;
	public AstExp initialValue;
	
	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstDecVar(String type, String name, AstExp initialValue)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();

		this.type = type;
		this.name = name;
		this.initialValue = initialValue;
	}

	/************************************************************/
	/* The printing message for a variable declaration AST node */
	/************************************************************/
	public void printMe()
	{
		/****************************************/
		/* AST NODE TYPE = AST VAR DECLARATION */
		/***************************************/
		if (initialValue != null) System.out.format("VAR-DEC(%s):%s := initialValue\n",name,type);
		if (initialValue == null) System.out.format("VAR-DEC(%s):%s                \n",name,type);

		/**************************************/
		/* RECURSIVELY PRINT initialValue ... */
		/**************************************/
		if (initialValue != null) initialValue.printMe();

		/**********************************/
		/* PRINT to AST GRAPHVIZ DOT file */
		/**********************************/
		AstGraphviz.getInstance().logNode(
                serialNumber,
			String.format("VAR\nDEC(%s)\n:%s",name,type));

		/****************************************/
		/* PRINT Edges to AST GRAPHVIZ DOT file */
		/****************************************/
		if (initialValue != null) AstGraphviz.getInstance().logEdge(serialNumber,initialValue.serialNumber);
			
	}

	public Type semantMe()
	{
		Type t;
	
		/****************************/
		/* [1] Check If Type exists */
		/****************************/
		t = SymbolTable.getInstance().find(type);
		if (t == null)
		{
			System.out.format(">> ERROR [%d:%d] non existing type %s\n",2,2,type);
			System.exit(0);
		}
		
		/**************************************/
		/* [2] Check That Name does NOT exist */
		/**************************************/
		if (SymbolTable.getInstance().find(name) != null)
		{
			System.out.format(">> ERROR [%d:%d] variable %s already exists in scope\n",2,2,name);				
		}

		/************************************************/
		/* [3] Enter the Identifier to the Symbol Table */
		/************************************************/
		SymbolTable.getInstance().enter(name,t);

		/************************************************************/
		/* [4] Return value is irrelevant for variable declarations */
		/************************************************************/
		return null;		
	}

	public Temp irMe()
	{
		Ir.getInstance().AddIrCommand(new IrCommandAllocate(name));

		if (initialValue != null)
		{
			Ir.getInstance().AddIrCommand(new IrCommandStore(name,initialValue.irMe()));
		}
		return null;
	}
	
}
