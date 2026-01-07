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

public class IrCommandStore extends IrCommand
{
	String varName;
	Temp src;
	
	public IrCommandStore(String varName, Temp src)
	{
		this.src      = src;
		this.varName = varName;
	}
	
	/***************/
	/* MIPS me !!! */
	/***************/
	public void mipsMe()
	{
		MipsGenerator.getInstance().store(varName,src);
	}
}
