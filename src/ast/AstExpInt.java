package ast;

import types.*;
import temp.*;
import ir.*;

public class AstExpInt extends AstExp
{
	public int value;
	
	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstExpInt(int value)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();
		this.value = value;
	}

	/************************************************/
	/* The printing message for an INT EXP AST node */
	/************************************************/
	public void printMe()
	{
		/*******************************/
		/* AST NODE Type = AST INT EXP */
		/*******************************/
		System.out.format("AST NODE INT( %d )\n",value);

		/***************************************/
		/* PRINT Node to AST GRAPHVIZ DOT file */
		/***************************************/
		AstGraphviz.getInstance().logNode(
                serialNumber,
			String.format("INT(%d)",value));
	}

	public Type semantMe()
	{
		return TypeInt.getInstance();
	}
	
	/*****************/
	/* IR ME         */
	/*****************/
	@Override
	public Temp irMe()
	{
		/**************************************/
		/* [1] Create a fresh temporary       */
		/**************************************/
		Temp t = TempFactory.getInstance().getFreshTemp();

		/**************************************/
		/* [2] Emit IR command to load the    */
		/*     integer constant into the temp */
		/**************************************/
		Ir.getInstance().AddIrCommand(new IRcommandConstInt(t, value));

		/**************************************/
		/* [3] Return the temporary           */
		/**************************************/
		return t;
	}
}
