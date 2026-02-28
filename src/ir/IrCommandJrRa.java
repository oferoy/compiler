package ir;

import mips.*;

/** IR command: emit jr $ra (return from function). */
public class IrCommandJrRa extends IrCommand
{
	@Override
	public void mipsMe()
	{
		MipsGenerator.getInstance().jrRa();
	}
}
