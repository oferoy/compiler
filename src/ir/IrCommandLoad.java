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

public class IrCommandLoad extends IrCommand
{
	Temp dst;
	String varName;
	
	public IrCommandLoad(Temp dst, String varName)
	{
		this.dst      = dst;
		this.varName = varName;
	}
	
	/***************/
	/* MIPS me !!! */
	/***************/
	public void mipsMe()
	{
		MipsGenerator.getInstance().load(dst, varName);
	}
}
