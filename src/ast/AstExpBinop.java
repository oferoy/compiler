package ast;

import types.*;
import temp.*;
import ir.*;

public class AstExpBinop extends AstExp
{
	int op;
	public AstExp left;
	public AstExp right;
	
	/******************/
	/* CONSTRUCTOR(S) */
	/******************/
	public AstExpBinop(AstExp left, AstExp right, int op)
	{
		/******************************/
		/* SET A UNIQUE SERIAL NUMBER */
		/******************************/
		serialNumber = AstNodeSerialNumber.getFresh();

		/***************************************/
		/* PRINT CORRESPONDING DERIVATION RULE */
		/***************************************/
		System.out.print("====================== exp -> exp BINOP exp\n");

		/*******************************/
		/* COPY INPUT DATA MENBERS ... */
		/*******************************/
		this.left = left;
		this.right = right;
		this.op = op;
	}
	
	/*************************************************/
	/* The printing message for a binop exp AST node */
	/*************************************************/
	public void printMe()
	{
		String sop="";
		
		/*********************************/
		/* CONVERT OP to a printable sop */
		/*********************************/
		if (op == 0) {sop = "+";}
		if (op == 1) {sop = "-";}
		if (op == 3) {sop = "=";}

		/**********************************/
		/* AST NODE TYPE = AST BINOP EXP */
		/*********************************/
		System.out.print("AST NODE BINOP EXP\n");
		System.out.format("BINOP EXP(%s)\n",sop);

		/**************************************/
		/* RECURSIVELY PRINT left + right ... */
		/**************************************/
		if (left != null) left.printMe();
		if (right != null) right.printMe();

		/***************************************/
		/* PRINT Node to AST GRAPHVIZ DOT file */
		/***************************************/
		AstGraphviz.getInstance().logNode(
                serialNumber,
			String.format("BINOP(%s)",sop));
		
		/****************************************/
		/* PRINT Edges to AST GRAPHVIZ DOT file */
		/****************************************/
		if (left  != null) AstGraphviz.getInstance().logEdge(serialNumber,left.serialNumber);
		if (right != null) AstGraphviz.getInstance().logEdge(serialNumber,right.serialNumber);
	}

	public Type semantMe()
	{
		Type t1 = null;
		Type t2 = null;
		
		if (left  != null) t1 = left.semantMe();
		if (right != null) t2 = right.semantMe();
		
		if ((t1 == TypeInt.getInstance()) && (t2 == TypeInt.getInstance()))
		{
			return TypeInt.getInstance();
		}
		System.exit(0);
		return null;
	}

	public Temp irMe()
	{
		Temp t1 = null;
		Temp t2 = null;
		Temp dst = TempFactory.getInstance().getFreshTemp();

		if (left  != null) t1 = left.irMe();
		if (right != null) t2 = right.irMe();

		if (op == 0)
		{
			Ir.
					getInstance().
					AddIrCommand(new IrCommandBinopAddIntegers(dst,t1,t2));
		}
		if (op == 2)
		{
			Ir.
					getInstance().
					AddIrCommand(new IrCommandBinopMulIntegers(dst,t1,t2));
		}
		if (op == 3)
		{
			Ir.
					getInstance().
					AddIrCommand(new IrCommandBinopEqIntegers(dst,t1,t2));
		}
		if (op == 4)
		{
			Ir.
					getInstance().
					AddIrCommand(new IrCommandBinopLtIntegers(dst,t1,t2));
		}
		return dst;
	}

}
