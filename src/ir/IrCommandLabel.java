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

public class IrCommandLabel extends IrCommand
{
	String labelName;

	public IrCommandLabel(String labelName)
	{
		this.labelName = labelName;
	}

	@Override
	public String getLabelName() { return labelName; }

	@Override
	public void mipsMe() { mips.MipsGenerator.getInstance().label(labelName); }

	@Override
	public String toString()
	{
		return "LABEL " + labelName + ":";
	}
}
