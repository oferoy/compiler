/***********/
/* PACKAGE */
/***********/
package ir;

/*******************/
/* GENERAL IMPORTS */
/*******************/

/*******************/
/* PROJECT IMPORTS */
/*******************/
import temp.*;
import mips.*;

public class IrCommandBinopEqIntegers extends IrCommand
{
	public Temp t1;
	public Temp t2;
	public Temp dst;

	public IrCommandBinopEqIntegers(Temp dst, Temp t1, Temp t2)
	{
		this.dst = dst;
		this.t1 = t1;
		this.t2 = t2;
	}
	
	/***************/
	/* MIPS me !!! */
	/***************/
	public void mipsMe()
	{
		/*******************************/
		/* [1] Allocate 3 fresh labels */
		/*******************************/
		String labelEnd        = getFreshLabel("end");
		String labelAssignOne  = getFreshLabel("AssignOne");
		String labelAssignZero = getFreshLabel("AssignZero");
		
		/******************************************/
		/* [2] if (t1==t2) goto label_AssignOne;  */
		/*     if (t1!=t2) goto label_AssignZero; */
		/******************************************/
		MipsGenerator.getInstance().beq(t1,t2,labelAssignOne);
		MipsGenerator.getInstance().bne(t1,t2,labelAssignZero);

		/************************/
		/* [3] label_AssignOne: */
		/*                      */
		/*         t3 := 1      */
		/*         goto end;    */
		/*                      */
		/************************/
		MipsGenerator.getInstance().label(labelAssignOne);
		MipsGenerator.getInstance().li(dst,1);
		MipsGenerator.getInstance().jump(labelEnd);

		/*************************/
		/* [4] label_AssignZero: */
		/*                       */
		/*         t3 := 1       */
		/*         goto end;     */
		/*                       */
		/*************************/
		MipsGenerator.getInstance().label(labelAssignZero);
		MipsGenerator.getInstance().li(dst,0);
		MipsGenerator.getInstance().jump(labelEnd);

		/******************/
		/* [5] label_end: */
		/******************/
		MipsGenerator.getInstance().label(labelEnd);
	}
}
