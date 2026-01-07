package ast;

import temp.*;
import ir.*;

public class AstStmtWhile extends AstStmt
{
	public AstExp cond;
	public AstStmtList body;

	/*******************/
	/*  CONSTRUCTOR(S) */
	/*******************/
	public AstStmtWhile(AstExp cond, AstStmtList body)
	{
		this.cond = cond;
		this.body = body;
	}

	public Temp irMe()
	{
		/*******************************/
		/* [1] Allocate 2 fresh labels */
		/*******************************/
		String labelEnd   = IrCommand.getFreshLabel("end");
		String labelStart = IrCommand.getFreshLabel("start");

		/*********************************/
		/* [2] entry label for the while */
		/*********************************/
		Ir.
				getInstance().
				AddIrCommand(new IrCommandLabel(labelStart));

		/********************/
		/* [3] cond.IRme(); */
		/********************/
		Temp condTemp = cond.irMe();

		/******************************************/
		/* [4] Jump conditionally to the loop end */
		/******************************************/
		Ir.
				getInstance().
				AddIrCommand(new IrCommandJumpIfEqToZero(condTemp,labelEnd));

		/*******************/
		/* [5] body.IRme() */
		/*******************/
		body.irMe();

		/******************************/
		/* [6] Jump to the loop entry */
		/******************************/
		Ir.
				getInstance().
				AddIrCommand(new IrCommandJumpLabel(labelStart));

		/**********************/
		/* [7] Loop end label */
		/**********************/
		Ir.
				getInstance().
				AddIrCommand(new IrCommandLabel(labelEnd));

		/*******************/
		/* [8] return null */
		/*******************/
		return null;
	}
}