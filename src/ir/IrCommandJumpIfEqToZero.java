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

public class IrCommandJumpIfEqToZero extends IrCommand
{
	Temp t;
	String labelName;
	
	public IrCommandJumpIfEqToZero(Temp t, String labelName)
	{
		this.t          = t;
		this.labelName = labelName;
	}
	
	/***************/
	/* MIPS me !!! */
	/***************/
	public void mipsMe()
	{
		MipsGenerator.getInstance().beqz(t, labelName);
	}
}
