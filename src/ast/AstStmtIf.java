package ast;

import types.*;
import symboltable.*;
import temp.*;
import ir.*;

public class AstStmtIf extends AstStmt
{
	public AstExp cond;
	public AstStmtList thenBody;
	public AstStmtList elseBody;

	/*******************/
	/*  CONSTRUCTOR(S) */
	/*******************/
	// Constructor for: IF (cond) { thenBody }
	public AstStmtIf(AstExp cond, AstStmtList thenBody)
	{
		this(cond, thenBody, null);
	}

	// Constructor for: IF (cond) { thenBody } ELSE { elseBody }
	public AstStmtIf(AstExp cond, AstStmtList thenBody, AstStmtList elseBody)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();

		this.cond = cond;
		this.thenBody = thenBody;
		this.elseBody = elseBody;
	}

	/****************************************************/
	/* The printing message for an if statement AST node */
	/****************************************************/
	public void printMe()
	{
		/*************************************/
		/* AST NODE Type = AST IF STATEMENT */
		/*************************************/
		if (elseBody != null)
		{
			System.out.print("AST NODE STMT IF-ELSE\n");
		}
		else
		{
			System.out.print("AST NODE STMT IF\n");
		}

		/**************************************/
		/* RECURSIVELY PRINT cond, then, else */
		/**************************************/
		if (cond != null) cond.printMe();
		if (thenBody != null) thenBody.printMe();
		if (elseBody != null) elseBody.printMe();

		/***************************************/
		/* PRINT Node to AST GRAPHVIZ DOT file */
		/***************************************/
		if (elseBody != null)
		{
			AstGraphviz.getInstance().logNode(
				serialNumber,
				"IF (cond)\nTHEN\nELSE");
		}
		else
		{
			AstGraphviz.getInstance().logNode(
				serialNumber,
				"IF (cond)\nTHEN");
		}
		
		/****************************************/
		/* PRINT Edges to AST GRAPHVIZ DOT file */
		/****************************************/
		if (cond != null) 
			AstGraphviz.getInstance().logEdge(serialNumber, cond.serialNumber);
		if (thenBody != null) 
			AstGraphviz.getInstance().logEdge(serialNumber, thenBody.serialNumber);
		if (elseBody != null)
			AstGraphviz.getInstance().logEdge(serialNumber, elseBody.serialNumber);
	}

	public Type semantMe()
	{
		// 1. Semant the condition
		Type condType = cond.semantMe();

		if (!(condType instanceof TypeInt)) {
			AstNode.error(lineNumber, 
				"IF condition must be of type int (boolean)");
		}

		// 2. Open a new scope for the IF body
		SymbolTable.beginScope();

		// 3. Semant the THEN body
		if (thenBody != null) {
			thenBody.semantMe();
		}

		// 4. Close the THEN scope
		SymbolTable.endScope();

		// 5. If there's an ELSE, semant it in its own scope
		if (elseBody != null)
		{
			SymbolTable.beginScope();
			elseBody.semantMe();
			SymbolTable.endScope();
		}

		return null;  // IF has no type
	}

	/*****************/
	/* IR ME         */
	/*****************/
	@Override
	public Temp irMe()
	{
		if (elseBody == null)
		{
			// Simple IF without ELSE
			/**************************************/
			/* [1] Generate fresh label for end  */
			/**************************************/
			String labelEnd = IrCommand.getFreshLabel("if_end");

			/**************************************/
			/* [2] Generate IR for condition     */
			/**************************************/
			Temp condTemp = cond.irMe();

			/**************************************/
			/* [3] Jump to end if condition is   */
			/*     false (equals zero)            */
			/**************************************/
			Ir.getInstance().AddIrCommand(
				new IrCommandJumpIfEqToZero(condTemp, labelEnd));

			/**************************************/
			/* [4] Generate IR for THEN body     */
			/**************************************/
			if (thenBody != null)
			{
				// Begin scope for VarNameMapper (to handle shadowing)
				VarNameMapper.getInstance().beginScope();
				
				thenBody.irMe();
				
				// End scope for VarNameMapper
				VarNameMapper.getInstance().endScope();
			}

			/**************************************/
			/* [5] Place end label                */
			/**************************************/
			Ir.getInstance().AddIrCommand(
				new IrCommandLabel(labelEnd));
		}
		else
		{
			// IF-ELSE with both branches
			/**************************************/
			/* [1] Generate fresh labels         */
			/**************************************/
			String labelElse = IrCommand.getFreshLabel("if_else");
			String labelEnd = IrCommand.getFreshLabel("if_end");

			/**************************************/
			/* [2] Generate IR for condition     */
			/**************************************/
			Temp condTemp = cond.irMe();

			/**************************************/
			/* [3] Jump to ELSE if condition is  */
			/*     false (equals zero)            */
			/**************************************/
			Ir.getInstance().AddIrCommand(
				new IrCommandJumpIfEqToZero(condTemp, labelElse));

			/**************************************/
			/* [4] Generate IR for THEN body     */
			/**************************************/
			if (thenBody != null)
			{
				// Begin scope for VarNameMapper (to handle shadowing)
				VarNameMapper.getInstance().beginScope();
				
				thenBody.irMe();
				
				// End scope for VarNameMapper
				VarNameMapper.getInstance().endScope();
			}

			/**************************************/
			/* [5] Jump to end (skip ELSE)       */
			/**************************************/
			Ir.getInstance().AddIrCommand(
				new IrCommandJumpLabel(labelEnd));

			/**************************************/
			/* [6] Place ELSE label               */
			/**************************************/
			Ir.getInstance().AddIrCommand(
				new IrCommandLabel(labelElse));

			/**************************************/
			/* [7] Generate IR for ELSE body     */
			/**************************************/
			// Begin scope for VarNameMapper (to handle shadowing)
			VarNameMapper.getInstance().beginScope();
			
			elseBody.irMe();
			
			// End scope for VarNameMapper
			VarNameMapper.getInstance().endScope();

			/**************************************/
			/* [8] Place end label                */
			/**************************************/
			Ir.getInstance().AddIrCommand(
				new IrCommandLabel(labelEnd));
		}

		/**************************************/
		/* Return null (statements don't     */
		/* return values)                     */
		/**************************************/
		return null;
	}

}
