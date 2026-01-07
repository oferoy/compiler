package ast;

import types.*;
import symboltable.*;
import ir.*;
import temp.*;

public class AstExpVarSimple extends AstExpVar
{
	/************************/
	/* simple variable name */
	/************************/
	public String name;
	
	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstExpVarSimple(String name)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();

		System.out.format("====================== var -> ID( %s )\n",name);
		this.name = name;
	}

	/**************************************************/
	/* The printing message for a simple var AST node */
	/**************************************************/
	public void printMe()
	{
		/**********************************/
		/* AST NODE TYPE = AST SIMPLE VAR */
		/**********************************/
		System.out.format("AST NODE SIMPLE VAR( %s )\n",name);

		/***************************************/
		/* PRINT Node to AST GRAPHVIZ DOT file */
		/***************************************/
		AstGraphviz.getInstance().logNode(
                serialNumber,
			String.format("SIMPLE\nVAR\n(%s)",name));
	}

	public Type semantMe()
	{
		return SymbolTable.getInstance().find(name);
	}

	public Temp irMe()
	{
		Temp t = TempFactory.getInstance().getFreshTemp();
		Ir.getInstance().AddIrCommand(new IrCommandLoad(t,name));
		return t;
	}
}
