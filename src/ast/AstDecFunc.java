package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstDecFunc extends AstDec
{
	/****************/
	/* DATA MEMBERS */
	/****************/
	public String returnTypeName;
	public String name;
	public AstTypeNameList params;
	public AstStmtList body;
	
	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstDecFunc(
		String returnTypeName,
		String name,
		AstTypeNameList params,
		AstStmtList body)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();

		this.returnTypeName = returnTypeName;
		this.name = name;
		this.params = params;
		this.body = body;
	}

	/************************************************************/
	/* The printing message for a function declaration AST node */
	/************************************************************/
	public void printMe()
	{
		/*************************************************/
		/* AST NODE TYPE = AST NODE FUNCTION DECLARATION */
		/*************************************************/
		System.out.format("FUNC(%s):%s\n",name,returnTypeName);

		/***************************************/
		/* RECURSIVELY PRINT params + body ... */
		/***************************************/
		if (params != null) params.printMe();
		if (body   != null) body.printMe();
		
		/***************************************/
		/* PRINT Node to AST GRAPHVIZ DOT file */
		/***************************************/
		AstGraphviz.getInstance().logNode(
                serialNumber,
			String.format("FUNC(%s)\n:%s\n",name,returnTypeName));
		
		/****************************************/
		/* PRINT Edges to AST GRAPHVIZ DOT file */
		/****************************************/
		if (params != null) AstGraphviz.getInstance().logEdge(serialNumber,params.serialNumber);
		if (body   != null) AstGraphviz.getInstance().logEdge(serialNumber,body.serialNumber);
	}

	public Type semantMe()
	{
		Type t;
		Type returnType = null;
		TypeList type_list = null;

		/*******************/
		/* [0] return type */
		/*******************/
		returnType = SymbolTable.getInstance().find(returnTypeName);
		if (returnType == null)
		{
			System.out.format(">> ERROR [%d:%d] non existing return type %s\n",6,6,returnType);				
		}
	
		/****************************/
		/* [1] Begin Function Scope */
		/****************************/
		SymbolTable.getInstance().beginScope();

		/***************************/
		/* [2] Semant Input Params */
		/***************************/
		for (AstTypeNameList it = params; it  != null; it = it.tail)
		{
			t = SymbolTable.getInstance().find(it.head.type);
			if (t == null)
			{
				System.out.format(">> ERROR [%d:%d] non existing type %s\n",2,2,it.head.type);				
			}
			else
			{
				type_list = new TypeList(t,type_list);
				SymbolTable.getInstance().enter(it.head.name,t);
			}
		}

		/*******************/
		/* [3] Semant Body */
		/*******************/
		body.semantMe();

		/*****************/
		/* [4] End Scope */
		/*****************/
		SymbolTable.getInstance().endScope();

		/***************************************************/
		/* [5] Enter the Function Type to the Symbol Table */
		/***************************************************/
		SymbolTable.getInstance().enter(name,new TypeFunction(returnType,name,type_list));

		/************************************************************/
		/* [6] Return value is irrelevant for function declarations */
		/************************************************************/
		return null;		
	}

	public Temp irMe()
	{
		Ir.
				getInstance().
				AddIrCommand(new IrCommandLabel("main"));
		if (body != null) body.irMe();

		return null;
	}
	
}
