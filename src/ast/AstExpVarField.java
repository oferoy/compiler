package ast;

import types.*;

public class AstExpVarField extends AstExpVar
{
	public AstExpVar var;
	public String fieldName;
	
	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstExpVarField(AstExpVar var, String fieldName)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();

		System.out.format("====================== var -> var DOT ID( %s )\n",fieldName);
		this.var = var;
		this.fieldName = fieldName;
	}

	/*************************************************/
	/* The printing message for a field var AST node */
	/*************************************************/
	public void printMe()
	{
		/*********************************/
		/* AST NODE TYPE = AST FIELD VAR */
		/*********************************/
		System.out.format("FIELD\nNAME\n(___.%s)\n",fieldName);

		/**********************************************/
		/* RECURSIVELY PRINT VAR, then FIELD NAME ... */
		/**********************************************/
		if (var != null) var.printMe();

		/**********************************/
		/* PRINT to AST GRAPHVIZ DOT file */
		/**********************************/
		AstGraphviz.getInstance().logNode(
                serialNumber,
			String.format("FIELD\nVAR\n___.%s",fieldName));

		/****************************************/
		/* PRINT Edges to AST GRAPHVIZ DOT file */
		/****************************************/
		if (var  != null) AstGraphviz.getInstance().logEdge(serialNumber,var.serialNumber);
	}

	public Type semantMe()
	{
		Type t = null;
		TypeClass tc = null;
		
		/******************************/
		/* [1] Recursively semant var */
		/******************************/
		if (var != null) t = var.semantMe();
		
		/*********************************/
		/* [2] Make sure type is a class */
		/*********************************/
		if (t.isClass() == false)
		{
			System.out.format(">> ERROR [%d:%d] access %s field of a non-class variable\n",6,6,fieldName);
			System.exit(0);
		}
		else
		{
			tc = (TypeClass) t;
		}
		
		/************************************/
		/* [3] Look for fiedlName inside tc */
		/************************************/
		for (TypeList it = tc.dataMembers; it != null; it=it.tail)
		{
			if (it.head.name == fieldName)
			{
				return it.head;
			}
		}
		
		/*********************************************/
		/* [4] fieldName does not exist in class var */
		/*********************************************/
		System.out.format(">> ERROR [%d:%d] field %s does not exist in class\n",6,6,fieldName);							
		System.exit(0);
		return null;
	}
}
