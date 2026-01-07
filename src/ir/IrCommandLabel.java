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

public class IrCommandLabel extends IrCommand
{
	String labelName;
	
	public IrCommandLabel(String labelName)
	{
		this.labelName = labelName;
	}
	
	/***************/
	/* MIPS me !!! */
	/***************/
	public void mipsMe()
	{
		MipsGenerator.getInstance().label(labelName);
	}
}
