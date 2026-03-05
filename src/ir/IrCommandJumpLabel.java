/***********/
/* PACKAGE */
/***********/
package ir;

import mips.*;

public class IrCommandJumpLabel extends IrCommand
{
	String labelName;

	public IrCommandJumpLabel(String labelName)
	{
		this.labelName = labelName;
	}

	@Override
	public String getJumpLabel() { return labelName; }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().jump(labelName); }
}
