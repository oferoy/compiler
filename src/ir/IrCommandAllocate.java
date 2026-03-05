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

public class IrCommandAllocate extends IrCommand
{
	String varName;

	public IrCommandAllocate(String varName)
	{
		this.varName = varName;
	}

	public String getVarName() { return varName; }

	@Override
	public void mipsMe() { mips.MipsGenerator.getInstance().allocate(varName); }

	@Override
	public String toString()
	{
		return "ALLOCATE " + varName;
	}
}
