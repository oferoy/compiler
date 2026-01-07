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
import mips.*;

public class IrCommandAllocate extends IrCommand
{
	String varName;
	
	public IrCommandAllocate(String varName)
	{
		this.varName = varName;
	}
	
	/***************/
	/* MIPS me !!! */
	/***************/
	public void mipsMe()
	{
		MipsGenerator.getInstance().allocate(varName);
	}
}
