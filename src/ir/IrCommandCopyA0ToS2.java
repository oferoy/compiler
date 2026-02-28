package ir;

import mips.*;

/** Copy $a0 to $s2 at method entry so receiver survives in callee-saved reg (avoids wrong-slot / clobber issues). */
public class IrCommandCopyA0ToS2 extends IrCommand
{
	@Override
	public void mipsMe() {
		MipsGenerator.getInstance().copyA0ToS2();
	}
}
